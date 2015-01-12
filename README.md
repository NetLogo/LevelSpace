# Index
[General](#general)
[LevelSpace fundamentals](#levelspace-fundamentals)
[Primitives](#primitives)
- [Opening and Closing Models] (#Opening and Closing Models)
    - [load-headless-model] (#lsload-headless-model-path)
    - [load-gui-model](#lsload-gui-model-path)
    - [last-model-id](#last-model-id)
- [Command and Reporting models] (#Command and Reporting models)
    - [ask](#lsask-model-id--list-string-of-commands)
    - [of](#reporter-string-lsof-model-id--list)
    - [ask-descendent](#lsask-descendent-list-string-of-commands)
    - [of-descendent](#reporter-string-lsof-descendent-list)
- [Logic & Control] (#Logic & Control)

# General

LevelSpace is an extension for NetLogo that allows you to run several models concurrently and have them talk with each other. LevelSpace models are hierarchical, meaning that a model has child models. In this documentation, we will refer to models that have loaded LevelSpace and have opened models as 'parents', and to the models they have opened as 'children' or 'child models'.

## LevelSpace fundamentals

LevelSpace must be loaded in a model using the ```extensions [ls]``` command. Once this is done, a model will be able to load up other models using the LevelSpace primitives, run commands and reporters in them, and close them down when they are no longer needed.

Asking and reporting in LevelSpace is concetually pretty straight forward: You pass strings to child models, and the child models respond as if you had typed that string into their Command Center. For this reason, all commands and reporters can be run in any context (they are OTPL). LevelSpace allows you to report strings, numbers, and lists from a child to its parent. It is not possible to directly report turtles, patches, links, or any of their respective sets. Further, it is not possible to push data from a child to its parent - parents must ask their children to report.

LevelSpace has two different child model types, headless models and GUI models. They each have their strengths and weaknesses: Headless models are slightly faster than GUI models (about 10-15%). GUI models allow you full access to a model's view, its interface + widgets, and its Command Center. Typically you will want to use Headless models when you are running a large number of models, or if you simply want to run them faster - GUI models are good if you run a small amount of models, or if you are writing a LevelSpace model and need to be able to debug.

Child models are kept track of in the extension with a serial number, starting with 0, and all communication from parent to child is done by referencing this number, henceforth referred to as ```model-id```. 

The easiest way to work with multiple models is to store their ```model-id``` in a list, and use NetLogo's list primitives to sort, filter, etc. them during runtime.

## Primitives
### Opening and Closing Models

Both of these commands will take a full, absolute path to a .nlogo model.

####`ls:load-gui-model` _path_
####`ls:load-headless-model` _path_

To get the ID of the last model you opened, this will report the ```model-id``` of the last model created in LevelSpace.

####`ls:last-model-id`

This command will close a model with the given ID.

####`ls:close-model` _model-id_

### Command and Reporting models

There are two different ways to ask child models to do things; either by providing a list of model-ids, or by providing just one model-id.

####`ls:ask` (_model-id_ | _list_) _string-of-commands_

Similarly, you can report from a child model using 

####_reporter-string_ `ls:of` (_model-id_ | _list_)

Sometimes you'll want grandchildren or child models even further down the hierarchy to do or report things. LevelSpace has special hierarchical primitives for this purpose:

####`ls:ask-descendent` _list_ _string-of-commands_

####_reporter-string_ `ls:of-descendent` _list_

For the hierarchical primitives, the list is read from left to right, and the reporter or command is passed down through the hierarchy. For instance, if we want to ask model 0's child model 1 to ask its child model 9 to call its `setup`, we would write

####`ls:ask-descendent [0 1 9] "setup"`

### Logic & Control
LevelSpace provides a variety of primitives for keeping track of your models. Depending on your purpose, some of these might be useful, others might not be.

####`ls:reset`

This command closes down all child models (and, recursively, their child models). You'll basically always want to call this in your setup procedure.

####`ls:show` _model-id_

####`ls:hide` _model-id_

These commands respectively show and hide GUI models. This is useful for when your models are cluttering your screen, or for running the models faster, since there is no overhead for drawing the models' views.

####`ls:path-of` _model-id_

This reports the full path, including the .nlogo file name, of the model.

####`ls:name-of` _model-id_

This reports the name of the .nlogo file of the model.

####`ls:model-exists` _model-id_

This reports a boolean value for whether there is a model with that model-id. This is often useful when you are dynamically generating models, and want to make sure that you are not asking models that no longer exist to do stuff.
