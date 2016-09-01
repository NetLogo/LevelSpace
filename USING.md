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
