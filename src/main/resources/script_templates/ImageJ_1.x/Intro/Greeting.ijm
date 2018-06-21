/*
 * An ImageJ macro with parameters.
 * It is the duty of the scripting framework to harvest
 * the 'name' parameter from the user, and then display
 * the 'greeting' output parameter, based on its type.
 */

#@ String name
#@OUTPUT greeting

greeting = "Hello " + name + "!";
