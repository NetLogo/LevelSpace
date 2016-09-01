
# LS Extension for NetLogo

LevelSpace is an extension for NetLogo that allows you to run several models concurrently and have them talk with each other. LevelSpace models are hierarchical, meaning that a model has child models. In this documentation, we will refer to models that have loaded LevelSpace and have opened models as 'parents', and to the models they have opened as 'children' or 'child models'.

## LevelSpace fundamentals

LevelSpace must be loaded in a model using ```extensions [ls]``` at the top of your model. Once this is done, a model will be able to load up other models using the LevelSpace primitives, run commands and reporters in them, and close them down when they are no longer needed.

Asking and reporting in LevelSpace is conceptually pretty straight forward: You pass blocks of code to child models, and the child models respond as if you had typed that string into their Command Center. LevelSpace allows you to report strings, numbers, and lists from a child to its parent. It is not possible to directly report turtles, patches, links, or any of their respective sets. Further, it is not possible to push data from a child to its parent - parents must ask their children to report.

LevelSpace has two different child model types, headless models and interactive models. They each have their strengths and weaknesses: Headless models are faster and use less memory than interactive models. Interactive models allow you full access to their interface and widgets, whereas headless models only give you access to their view and command center. Typically you will want to use headless models when you are running a large number of models, or if you simply want to run them faster - interactive models are good if you run a small amount of models, or if you are writing a LevelSpace model and need to be able to debug.

Child models are kept track of in the extension with an id number, starting with 0, and all communication from parent to child is done by referencing this number, henceforth referred to as `model-id`.

The easiest way to work with multiple models is to store their `model-id` in a list, and use NetLogo's list primitives to sort, filter, etc. them during runtime.

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
    ls:ask ls:models [ go ]
    show mean [ count sheep ] ls:of ls:models
end
```

## Citing LevelSpace in Research

If you use LevelSpace in research, we ask that you cite us,

Hjorth, A.  Head, B. & Wilensky, U. (2015). “LevelSpace NetLogo extension”. http://ccl.northwestern.edu/rp/levelspace/index.shtml Evanston, IL: Center for Connected Learning and Computer Based Modeling, Northwestern University.


## Primitives

### Commanding and Reporting

[`ls:ask`](#lsask)
[`ls:of`](#lsof)
[`ls:report`](#lsreport)
[`ls:with`](#lswith)
[`ls:let`](#lslet)

### Logic and Control

[`ls:model`](#lsmodel)
[`ls:show`](#lsshow)
[`ls:show-all`](#lsshow-all)
[`ls:hide`](#lshide)
[`ls:hide-all`](#lshide-all)
[`ls:path-of`](#lspath-of)
[`ls:name-of`](#lsname-of)
[`ls:model-exists`](#lsmodel-exists)

### Opening and Closing Models

[`ls:create-models`](#lscreate-models)
[`ls:create-interactive-models`](#lscreate-interactive-models)
[`ls:close`](#lsclose)
[`ls:reset`](#lsreset)



### `ls:create-models`

```NetLogo
ls:create-models number path
ls:create-models number path anonymous command
```


Create the specified number of instances of the given .nlogo model.  The path can be absolute, or relative to the main model. Compared with `ls:create-interactive-models`, this primitive creates lightweight models that start hidden. You should use this primitive if you plan on having many instances of the given model. The models may be shown using `ls:show`; when visible, they will have a view and command center, but no other widgets.

If given a command, LevelSpace will call the command after loading each instance of the model withe model id as the argument. This allows you to easily store model ids in a variable or list when loading models, or do other initialization. For example, to store a model id in a variable, you can do:

```NetLogo
let model-id 0
(ls:create-models "My-Model.nlogo" [ set model-id ? ])
```



### `ls:create-interactive-models`

```NetLogo
ls:create-interactive-models number path
ls:create-interactive-models number path anonymous command
```


Like `ls:create-models`, create the specified number of instances of the given .nlogo model. Unlike `ls:create-models`, `ls:create-interactive-models` creates models which are visible by default, and have all widgets. You should use this primitive you plan on having only a handful of instances of the given model, and would like to be able to interact with the instances through their interfaces.



### `ls:close`

```NetLogo
ls:close model-id
```


Close the model with the given `model-id`.



### `ls:reset`

```NetLogo
ls:reset
```


Close down all child models (and, recursively, their child models). You'll often want to call this in your setup procedure.

Note that `clear-all` does *not* close LevelSpace models.



### `ls:ask`

```NetLogo
ls:ask model-or-list-of-models command argument
```


Tell the given child model or list of child models to run the given command. This is the main way you get child models to actually do things. For example:

```NetLogo
ls:ask model-id [ create-turtles 5 ]
```

You can also ask a list of models to all do the same thing:

```NetLogo
ls:ask ls:models [ create-turtles 5 ]
```

You may supply the command with arguments, just like you would with anonymous commands:

```NetLogo
let turtle-id 0
let speed 5
(ls:ask model-id [ [t s] -> ask turtle t [ fd s ] ] turtle-id speed)
```

Note that the commands cannot access variables in the parent model directly. You must either pass information in through arguments or using `ls:let`.



### `ls:of`

```NetLogo
ls:of reporter model-or-list-of-models
```


Run the given reporter in the given model and report the result.

`ls:of` is designed to work like NetLogo's inbuilt `of`: If you send `ls:of` a model-id, it will report the value of the reporter from that model. If you send it a list of model-ids, it will report a list of values of the reporter string from all models. You cannot pass arguments to `ls:of`, but you can use `ls:let`.

```NetLogo
[ count turtles ] ls:of model-id
```



### `ls:report`

```NetLogo
ls:report model-or-list-of-models reporter argument
```


Run the given reporter in the given model and report the result. This form exists to allow you to pass arguments to the reporter.

```NetLogo
let turtle-id 0
(ls:report model-id [ [ color ] of turtle ? ] turtle-id)
```



### `ls:with`

```NetLogo
ls:with list-of-models reporter
```


Reports a new list of models containing only those models that report `true` when they run the reporter block.

```NetLogo
ls:models ls:with [ count turtles > 100 ]
```



### `ls:let`

```NetLogo
ls:let variable-name value
```


Creates a variable containing the given data that can be accessed by the child models in this context.

```NetLogo
ls:let num-turtles count turtles
ask turtles [
  ls:ask brain-model [
    create-turtles num-turtles
  ]
]
```

`ls:let` works quite similar to `let` in that the variable is only locally accessible:

```NetLogo
ask turtles [
  ls:let my-color color
]
;; my-color is innaccessible here
```

`ls:let` is very similar to `let`, except in a few cases.

- `ls:let` will overwrite previous values in the variable

If you do

```NetLogo
ls:let my-var 5
ls:let my-var 6
```

`my-var` will be set equal to `6`. There is no `ls:set`.

- `ls:let` supports variable shadowing

If you do

```NetLogo
ls:let my-var 5
ask turtles [
  ls:let my-var 6
  ls:ask child-model [ show my-var ]
]
ls:ask child-model [ show my-var ]
```

`child-model` will show `6` and then `5`. This is known as [variable shadowing](https://en.wikipedia.org/wiki/Variable_shadowing).

- The parent model cannot directly read the value of an ls variable

For example, this does *not* work.

```NetLogo
ls:let my-var 5
show my-var
```

This is intentional. ls variables are meant to be used for sharing data with child models. The parent model already has access to the data.

Furthermore, changing the value of an ls let variable in a child model will not affect it in any other model. For example:

```NetLogo
ls:let my-var 0
ls:ask ls:models [
  set my-var my-var + 1
  show my-var
]
```

All models will print `1`.

- `ls:let` does not respect the scope of `if`, `when`, and `repeat`

This behavior should be considered a bug and not relied upon. It is an unfortunate consequence of the way the NetLogo engine works. Hopefully, we'll be able to correct this in a future version of NetLogo.

For example, this is allowable:

```NetLogo
if true [
  ls:let my-var 5
]
ls:ask child-model [ create-turtles my-var ]
```

The scope of `ask` is respected, however.



### `ls:model`

```NetLogo
ls:model
```


Report a list of model-ids for all existing models.



### `ls:show`

```NetLogo
ls:show model-or-list-of-models
```


Makes all of the given models visible.



### `ls:show-all`

```NetLogo
ls:show-all model-or-list-of-models
```


Makes all of the given models *and their descendents* visible.



### `ls:hide`

```NetLogo
ls:hide model-or-list-of-models
```


Hide all of the given models. Hiding models is a good way of making your simulation run faster.



### `ls:hide-all`

```NetLogo
ls:hide-all model-or-list-of-models
```


Hide all of the given models *and their descendents*. Hiding models is a good way of making your simulation run faster.



### `ls:path-of`

```NetLogo
ls:path-of model-or-list-of-models
```


Report the full path, including the .nlogo file name, of the model. If a list of models is given, a list of paths is reported.



### `ls:name-of`

```NetLogo
ls:name-of model-or-list-of-models
```


Reports the name of the .nlogo file of the model. This is the name of window in which the model appears when visible. If a list of models is given, a list of names is reported.



### `ls:model-exists`

```NetLogo
ls:model-exists model-or-list-of-models
```


Report a boolean value for whether there is a model with that model-id. This is often useful when you are dynamically generating models, and want to make sure that you are not asking models that no longer exist to do stuff.



## Terms of Use

Copyright 1999-2016 by Uri Wilensky.

This program is free software; you can redistribute it and/or modify it under the terms of the [GNU General Public License](http://www.gnu.org/copyleft/gpl.html) as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

