# Index
- [General](#general)
- [LevelSpace fundamentals](#levelspace-fundamentals)
- [Primitives](#primitives)
    - [Opening and Closing Models](#opening-and-closing-models)
        - [load-headless-model](#lsload-headless-model-path)
        - [load-gui-model](#lsload-gui-model-path)
        - [close](#lsclose-model-id)
        - [reset](#lsreset)
    - [Command and Reporting models](#command-and-reporting-models)
        - [ask](#lsask-model-id--list-string-of-commands)
        - [of](#reporter-string-lsof-model-id--list)
        - [ask-descendent](#lsask-descendent-list-string-of-commands)
        - [of-descendent](#reporter-string-lsof-descendent-list)
    - [Logic & Control](#logic-control)
        - [models](#lsmodels)
        - [show](#lsshow-model-id)
        - [hide](#lshide-model-id)
        - [path-of](#lspath-of-model-id)
        - [name-of](#lsname-of-model-id)
        - [model-exists?](#lsmodel-exists-model-id)
    - [Examples of use](#examples-of-use)

# General

LevelSpace is an extension for NetLogo that allows you to run several models concurrently and have them talk with each other. LevelSpace models are hierarchical, meaning that a model has child models. In this documentation, we will refer to models that have loaded LevelSpace and have opened models as 'parents', and to the models they have opened as 'children' or 'child models'.

## LevelSpace fundamentals

LevelSpace must be loaded in a model using the ```extensions [ls]``` command. Once this is done, a model will be able to load up other models using the LevelSpace primitives, run commands and reporters in them, and close them down when they are no longer needed.

Asking and reporting in LevelSpace is conceptually pretty straight forward: You pass blocks of code to child models, and the child models respond as if you had typed that string into their Command Center. LevelSpace allows you to report strings, numbers, and lists from a child to its parent. It is not possible to directly report turtles, patches, links, or any of their respective sets. Further, it is not possible to push data from a child to its parent - parents must ask their children to report.

LevelSpace has two different child model types, headless models and GUI models. They each have their strengths and weaknesses: Headless models are slightly faster than GUI models (about 10-15%). GUI models allow you full access to a model's view, its interface + widgets, and its Command Center. Typically you will want to use Headless models when you are running a large number of models, or if you simply want to run them faster - GUI models are good if you run a small amount of models, or if you are writing a LevelSpace model and need to be able to debug.

Child models are kept track of in the extension with an id number, starting with 0, and all communication from parent to child is done by referencing this number, henceforth referred to as ```model-id```.

The easiest way to work with multiple models is to store their ```model-id``` in a list, and use NetLogo's list primitives to sort, filter, etc. them during runtime.

## Primitives
### Opening and Closing Models

####`ls:load-gui-model` _path_

####(`ls:load-gui-model` _path_ _command-task_)

####`ls:load-headless-model` _path_

####(`ls:load-headless-model` _path_ _command-task_)

Load the given .nlogo model. The path can be absolute, or relative to the main model.

If given a command task, LevelSpace will call the command task after loading the model with the model id as the an
argument. This allows you to easily store model ids in a variable or list when loading models, or do other
intialization. For example, to store the model id in a variable, you can do:

    let model-id 0
    (ls:load-gui-model "My-Model.nlogo" [ set model-id ? ]

####`ls:close` _model-id_

Close the model with the given `model-id`.

####`ls:reset`

Close down all child models (and, recursively, their child models). You'll often want to call this in your setup procedure.


### Command and Reporting models

####`ls:ask` (_model-id_ | _list_) _command-block_

####(`ls:ask` (_model-id_ | _list_) _command-block_ _arguments_ ...)

Tell the given child model or list of child models to run the given command. This is the main way you get child models to actually do things. For example:

```
ls:ask model-id [ create-turtles 5 ]
```

You may also supply the command with arguments, just like you would with tasks:

```
let turtle-id 0
let speed 5
(ls:ask model-id [ ask turtle ?1 [ fd ?2 ] ] turtle-id speed)
```

####_reporter-block_ `ls:of` (_model-id_ | _list_)

Run the given reporter in the given model and report the result.

`ls:of` is designed to work like NetLogo's inbuilt `of`: If you send `ls:of` a model-id, it will report the value of the reporter from that model. If you send it a list of model-ids, it will report a list of values of the reporter string from all models. You cannot pass arguments to `ls:of`, but you can use `ls:let`.

```
[ count turtles ] ls:of model-id
```

####`ls:report` (_model-id_ | _list_) _reporter-block_

####(`ls:report` (_model-id_ | _list_) _reporter-block_ _arguments_ ...)

Run the given reporter in the given model and report the result. This form exists to allow you to pass arguments to the reporter.

```
let turtle-id 0
(ls:report model-id [ [ color ] of turtle ? ] turtle-id)
```

####`ls:ask-descendent` _list_ _string-of-commands_

####(`ls:ask-descendent` _list_ _string-of-commands_ _arguments_ ...)

####_reporter-string_ `ls:of-descendent` _list_

Like `ls:ask` and `ls:of`, but the list specifies a model at an arbitray place down the tree of models. This is useful when you child models that have child models that have child models and so forth.

For the hierarchical primitives, the list is read from left to right, and the reporter or command is passed down through the hierarchy. For instance, if we want to ask model 0's child model 1 to ask its child model 9 to call its `setup`, we would write

```
ls:ask-descendent [0 1 9] "setup"
```

####`ls:let` _variable-name_ _data_

Creates a variable containing the given data that can be accessed by the child models in this context.

```
ls:let num-turtles count turtles
ask turtles [
  ls:ask brain-model [
    create-turtles num-turtles
  ]
]
```

`ls:let` works quite similar to `let` in that the variable is only locally accessible:

```
ask turtles [
  ls:let my-color color
]
;; my-color is innaccessible here
```

### Logic & Control
LevelSpace provides a variety of primitives for keeping track of your models. Depending on your purpose, some of these might be useful, others might not be.

####`ls:models`

Report a list of model-ids for all existing models.

####`ls:show` _model-id_

####`ls:hide` _model-id_

Show and hide the given model. This is useful for when your models are cluttering your screen, or for running the models faster, since there is no overhead for drawing the models' views.

####`ls:path-of` _model-id_

Report the full path, including the .nlogo file name, of the model.

####`ls:name-of` _model-id_

Report the name of the .nlogo file of the model.

####`ls:model-exists` _model-id_

Report a boolean value for whether there is a model with that model-id. This is often useful when you are dynamically generating models, and want to make sure that you are not asking models that no longer exist to do stuff.

## Examples of use
Models are stored in lists, and we therefore need to use NetLogo's list primitives, rather than set primitives, when working with child models. The following contains best practice suggestions for doing so.

### A general usecase
A simple thing we can do is to open up some models, run them concurrently, and find out an average of some reporter. Let's say that we are interested in finding the mean of the number of sheep in a bunch of wolf sheep predation models. First we would open up some of these models, and set them up:

```
to setup
  ca
  ls:reset
  repeat 30 [ ls:load-headless-model "Wolf Sheep Predation.nlogo" ]
  ls:ask ls:models [ set grass? true setup ]
  reset-ticks
end
```
We then want to run all our child models, and then find out what the mean number of sheep is:
```
to go
    ls:ask ls:models "go"
    show mean [ count sheep ] ls:of ls:models
end
```

### 'with' in LevelSpace.
The best way to do the equivalent of `with` in LevelSpace is to combine `filter` with `ls:of`. Let's for instance say that we only want the models that satisfy a set of particular criteria. For example:

```
ls:ask (filter [[ count turtles > 5 ] ls:of ?]) [
  ask turtles [ fd 1 ]
]
```

### Caveats for `ls:let`

`ls:let` is very similar to `let`, except in a few cases.

#### `ls:let` will overwrite previous values in the variable

If you do

```
ls:let my-var 5
ls:let my-var 6
```

`my-var` will be set equal to `6`. There is no `ls:set`.

#### `ls:let` supports variable shadowing

If you do

```
ls:let my-var 5
ask turtles [
  ls:let my-var 6
  ls:ask child-model [ show my-var ]
]
ls:ask child-model [ show my-var ]
```

`child-model` will show `6` and then `5`. This is known as [variable shadowing](https://en.wikipedia.org/wiki/Variable_shadowing).

#### The parent model cannot directly read the value of an ls variable

For example, this does *not* work.

```
ls:let my-var 5
show my-var
```

This is intentional. ls variables are meant to be used for sharing data with child models. The parent model already has access to the data.

Furthermore, changing the value of an ls let variable in a child model will not affect it in any other model. For example:

```
ls:let my-var 0
ls:ask ls:models [
  set my-var my-var + 1
  show my-var
]
```

All models will print `1`.

#### `ls:let` does not respect the scope of `if`, `when`, and `repeat`

This behavior should be considered a bug and not relied upon. It is an unfortunate consequence of the way the NetLogo engine works. Hopefully, we'll be able to correct this in a future version of NetLogo.

For example, this is allowable:

```
if true [
  ls:let my-var 5
]
ls:ask child-model [ create-turtles my-var ]
```

The scope of `ask` is respected, however.

## Terms of Use

Copyright 1999-2014 by Uri Wilensky.

This program is free software; you can redistribute it and/or modify it under the terms of the [GNU General Public License](http://www.gnu.org/copyleft/gpl.html) as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
