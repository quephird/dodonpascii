# dodonpascii

## Description

This is my first attempt at a bullet-hell shmup using sprites made with ASCII characters. I had originally intended to render entities as actual text but doing things like collision detection and rotations would have required font metrics and tedious math that I just decided was not worth it. Images it is then.

## Getting it running

This project requires Leiningen; you can find instructions on how to install it here: http://www.leiningen.org/

Download the project to a local directory:

    git clone https://github.com/quephird/dodonpascii

... move into that directory and run the following:

    lein repl

Once in the Clojure REPL, issue the following:

    (load-file "./src/dodonpascii/core.clj")

## Current features

The following features have been implemented:

* A moving player
* Two types of baddies
* Firing bullets at the baddies
* Collision detection between player bullets and baddies
* Baddies have distinct attack patterns
* A mechanism for defining levels and waves of baddies
* Player bullets make sound
* Randomly generated shots powerups (this needs to change, see below)
* Shots increased when shot powerup absorbed

## Planned features

I am hoping to implement at least some of the following:

* Player needs to be constrained within margins
* Bullets from the baddies
* Collision detection between player and baddies
* Collision detection between player and baddies' bullets
* Sound from baddies' bullets
* Scoring when player shoots baddies
* Scoring when player grazes bullets
* Powerups dropped when entire group of special baddies is shot
* Multiple powerup types
* NEED MOAR BADDIES
* Levels, with increasing difficulty
* Bosses
* Bullet hell patterns
* Bonus ships
* High score maintenance
* Looping background music
* Scrolling background images
* Extra lives at critical scores

## Useful links

The fabulous quil library written in Clojure, https://github.com/quil/quil

Using quil in the functional mode, https://github.com/quil/quil/wiki/Functional-mode-(fun-mode)

The Processing environment: http://www.processing.org/

## License

Copyright (C) 2014, ⅅ₳ℕⅈⅇℒℒⅇ Ҝⅇℱℱoℜⅆ.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
