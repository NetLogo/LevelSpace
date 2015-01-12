# LevelsSpace

LevelSpace is an extension for NetLogo that allows you to run several models concurrently and have them talk with each other. LevelSpace models are hierarchical, meaning that a model has child models. In this documentation, we will refer to models that have loaded LevelSpace and have opened models as 'parents', and to the models they have opened as 'children' or 'child models'.

## LevelSpace fundamentals

LevelSpace must be loaded in a model using the ```extensions [ls]``` command. Once this is done, a model will be able to load up other models using the LevelSpace primitives, run commands and reporters in them, and close them down when they are no longer needed.

LevelSpace has two different child model types, headless models and GUI models. They each have their strengths and weaknesses: Headless models are slightly faster than GUI models (about 10-15%). GUI models allow you full access to a model's view, its interface + widgets, and its Command Center. Typically you will want to use Headless models when you are running a large number of models, or if you simply want to run them faster - GUI models are good if you run a small amount of models, or if you are writing a LevelSpace model and need to be able to debug.

LevelSpace allows you to report strings, numbers, and lists from a child to its parent. It is not possible to directly report turtles, patches, links, or any of their respective sets.

Child models are kept track of in the extension with a serial number, starting with 0, and all communication from parent to child is done by referencing this number, henceforth referred to as ```model-id```. 

The easiest way to work with multiple models is to store their ```model-id``` in a list, and use NetLogo's list primitives to sort, filter, etc. them during runtime.

## Primitives
### Opening and Closing Models

Both of these commands will take a full, absolute path to a .nlogo model.

```ls:load-gui-model``` *path* & ```ls:load-headless-model``` *path*

To get the ID of the last model you opened, use

```ls:last-model-id``` 

This command will close a model with the given ID.

```ls:close-model``` *model-id*

### Command and Reporting models

You run commands in a child model using

```ls:ask``` *model-id* *string-of-commands*

You can report from a child model using 

*reporter-string* ```ls:of``` *model-id*
