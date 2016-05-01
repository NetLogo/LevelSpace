package org.nlogo.ls

import scala.collection.breakOut
import scala.collection.JavaConverters._
import scala.collection.mutable.{Map => MMap, WeakHashMap}

import org.nlogo.api.{Context, Argument, Command, Reporter, ExtensionException, Dump}
import org.nlogo.core.{Syntax, LogoList, Let, Token, I18N}
import org.nlogo.nvm.{Context => NvmContext, ExtensionContext, Activation, LetBinding}

import com.google.common.collect.MapMaker


object CtxConverter {
  def nvm(ctx: Context): NvmContext = ctx.asInstanceOf[ExtensionContext].nvmContext
}

class ScopedVals(elems: (Activation, AnyRef)*) extends WeakHashMap[Activation, AnyRef] {
  elems foreach { case (k, v) => this(k) = v }
}

object LetPrim extends Command {

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
      .filter(b => b.let.name != null && b.let.name.startsWith(LetPrefix))
      .flatMap {
        case LetBinding(Let(name), m) =>
          toScopedVals(m).get(ctx.activation).map(name.substring(LetPrefix.length) -> _)
      }

  override def getSyntax = Syntax.commandSyntax(Array(Syntax.SymbolType, Syntax.ReadableType))

  override def perform(args: Array[Argument], ctx: Context) = {
    val token = args(0).getSymbol
    val let = Let(LetPrefix + token.text)
    val nvmCtx = CtxConverter.nvm(ctx)
    nvmCtx.letBindings.find(_.let.name equals let.name) match {
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

object Ask extends Command {
  override def getSyntax =
    Syntax.commandSyntax(right = List(Syntax.NumberType | Syntax.ListType,
                                      Syntax.CodeBlockType,
                                      Syntax.RepeatableType | Syntax.ReadableType),
                         defaultOption = Some(2))

  override def perform(args: Array[Argument], ctx: Context) = {
    val code = args(1).getCode.asScala.map(_.text).mkString(" ")
    val cmdArgs = args.slice(2, args.size).map(_.get)
    val lets = LetPrim.letBindings(CtxConverter.nvm(ctx))
    LevelSpace.toModelList(args(0)).map {
      _.ask(code, lets, cmdArgs)
    }.foreach(_(ctx.world))
  }
}

object Of extends Reporter {
  override def getSyntax =
    Syntax.reporterSyntax(left = Syntax.CodeBlockType,
                          right = List(Syntax.NumberType | Syntax.ListType),
                          ret = Syntax.ReadableType,
                          precedence = Syntax.NormalPrecedence + 1,
                          isRightAssociative = true)

  override def report(args: Array[Argument], ctx: Context): AnyRef =
    Report.report(Array(args(1), args(0)), ctx)
}

object Report extends Reporter {
  override def getSyntax =
    Syntax.reporterSyntax(right = List(Syntax.NumberType | Syntax.ListType,
                                       Syntax.CodeBlockType,
                                       Syntax.RepeatableType | Syntax.ReadableType),
                          ret = Syntax.ReadableType,
                          defaultOption = Some(2))

  override def report(args: Array[Argument], ctx: Context): AnyRef = {
    val code = args(1).getCode.asScala.map(_.text).mkString(" ")
    val cmdArgs = args.slice(2, args.size).map(_.get)
    val lets = LetPrim.letBindings(CtxConverter.nvm(ctx))
    val results = LevelSpace.toModelList(args(0)).map{
      _.of(code, lets, cmdArgs)
    }.map(_(ctx.world))
    if (args(0).get.isInstanceOf[Double]) results.head else LogoList.fromVector(results.toVector)
  }
}

object With extends Reporter {
  override def getSyntax =
    Syntax.reporterSyntax(left = Syntax.ListType,
                          right = List(Syntax.CodeBlockType),
                          ret = Syntax.ListType,
                          precedence = Syntax.NormalPrecedence + 2,
                          isRightAssociative = false)

  override def report(args: Array[Argument], ctx: Context): AnyRef = {
    val code = args(1).getCode.asScala.map(_.text).mkString(" ")
    val cmdArgs = args.slice(2, args.size).map(_.get)
    val lets = LetPrim.letBindings(CtxConverter.nvm(ctx))
    val matchingModels = LevelSpace.toModelList(args(0))
      .map(m => m -> m.of(code, lets, cmdArgs))
      .map(p => p._1 -> p._2(ctx.world))
      .filter {
        case (_, b: java.lang.Boolean) => b
        case (m: ChildModel, x: AnyRef) =>
          throw new ExtensionException(I18N.errorsJ.getN("org.nlogo.prim.$common.expectedBooleanValue",
                                                         "ls:with", m.name, Dump.logoObject(x)))
      }
      .map(_._1.getModelID: java.lang.Double)
      .toVector
    LogoList.fromVector(matchingModels)
  }
}


class ModelCommand(cmd: ChildModel => Unit) extends Command {
  override def getSyntax = Syntax.commandSyntax(Array(Syntax.NumberType | Syntax.ListType))
  override def perform(args: Array[Argument], ctx: Context): Unit = LevelSpace.toModelList(args(0)).foreach(cmd)
}

object Show extends ModelCommand(_.show)
object Hide extends ModelCommand(_.hide)
object Close extends ModelCommand(LevelSpace.closeModel _)
object UpdateView extends ModelCommand(_ match {
  case hm: HeadlessChildModel => hm.updateView
  case _ =>
})
