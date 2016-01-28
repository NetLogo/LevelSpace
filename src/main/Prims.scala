import scala.collection.breakOut
import scala.collection.JavaConverters._
import scala.collection.mutable.{Map => MMap, WeakHashMap}

import org.nlogo.api.{Syntax, Let, Context, Argument, DefaultCommand, DefaultReporter, Token, LogoList, ExtensionException}
import org.nlogo.nvm.{Context => NvmContext, ExtensionContext, Activation, LetBinding}

import com.google.common.collect.MapMaker


object CtxConverter {
  def nvm(ctx: Context): NvmContext = ctx.asInstanceOf[ExtensionContext].nvmContext
}

class ScopedVals(elems: (Activation, AnyRef)*) extends WeakHashMap[Activation, AnyRef] {
  elems foreach { case (k, v) => this(k) = v }
}

object LetPrim extends DefaultCommand {

  /**
   * In order to ensure no LS locals collide with regular locals in the
   * parent, we prefix with "ls ". This guarantees no collisions because
   * NetLogo locals are cannot contain space (and don't contain lower-case
   * letters, but I don't think that should be relied upon).
   * - BCH 10/11/2015
   **/
  val LetPrefix = "ls "

  def letBindings(ctx: NvmContext): Seq[(String, AnyRef)] =
    ctx.allLets
      .filter(b => b.let.varName != null && b.let.varName.startsWith(LetPrefix))
      .flatMap {
        case LetBinding(Let(name, _, _ ,_), m) =>
          toScopedVals(m).get(ctx.activation).map(name.substring(LetPrefix.length) -> _)
      }

  override def getSyntax = Syntax.commandSyntax(Array(Syntax.SymbolType, Syntax.ReadableType))

  override def perform(args: Array[Argument], ctx: Context) = {
    val token = args(0).getSymbol
    val let = Let(LetPrefix + token.name, token.startPos, token.endPos, List.empty[Let].asJava)
    val nvmCtx = CtxConverter.nvm(ctx)
    nvmCtx.letBindings.find(_.let.varName equals let.varName) match {
      // Note that we need to replace the value in the map is found since different scopes can have the same
      // Activation. `ask` is the most common instance of this. -- BCH 1/23/2016
      case Some(lb) => toScopedVals(lb.value)(nvmCtx.activation) = args(1).get
      case None     => nvmCtx.let(let, new ScopedVals(nvmCtx.activation -> args(1).get))
    }
  }

  def toScopedVals(x: AnyRef): ScopedVals = x match {
    case vba: ScopedVals => vba
    case _ => throw new ExtensionException("Something besides an activation map was found in an LS variable. This is a bug. Please report.")
  }
}

trait RunPrim {
  def run[T](ctx: Context, tokens: java.util.List[Token], args: Array[AnyRef], runFunc: (String, Array[AnyRef]) => T): T = {
    val bindings = LetPrim.letBindings(CtxConverter.nvm(ctx))
    val vars = bindings.zipWithIndex.map {
      case ((name, v), i) => name -> ("?" + (1 + args.length + i))
    }(breakOut): Map[String, String]


    val code = tokens.asScala.map { t => vars.getOrElse(t.name, t.name) }.mkString(" ")

    runFunc(code, args ++ bindings.map(_._2))
  }
}

object Ask extends DefaultCommand with RunPrim {
  override def getSyntax =
    Syntax.commandSyntax(Array(Syntax.NumberType | Syntax.ListType,
                               Syntax.CodeBlockType,
                               Syntax.RepeatableType | Syntax.ReadableType), 2)

  override def perform(args: Array[Argument], ctx: Context) =
    run(ctx, args(1).getCode, args.slice(2, args.size).map(_.get), (code, actuals) => {
      LevelSpace.toModelList(args(0)).foreach(_.ask(code, actuals))
    })
}

object Of extends DefaultReporter with RunPrim {
  override def getSyntax =
    Syntax.reporterSyntax(Syntax.CodeBlockType,
                          Array(Syntax.NumberType | Syntax.ListType),
                          Syntax.ReadableType,
                          Syntax.NormalPrecedence + 1,
                          true)

  override def report(args: Array[Argument], ctx: Context): AnyRef =
    Report.report(Array(args(1), args(0)), ctx)
}

object Report extends DefaultReporter with RunPrim {
  override def getSyntax =
    Syntax.reporterSyntax(Array(Syntax.NumberType | Syntax.ListType,
                                Syntax.CodeBlockType,
                                Syntax.RepeatableType | Syntax.ReadableType),
                          Syntax.ReadableType, 2)

  override def report(args: Array[Argument], ctx: Context): AnyRef =
    run(ctx, args(1).getCode, args.slice(2, args.size).map(_.get), (code, actuals) => {
      val results = LevelSpace.toModelList(args(0)).map(_.of(code, actuals)).toVector
      if (args(0).get.isInstanceOf[Double]) results.head else LogoList.fromVector(results)
    })
}


