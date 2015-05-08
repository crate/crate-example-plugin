.. image:: https://cdn.crate.io/web/2.0/img/crate-avatar_100x100.png
   :width: 100px
   :height: 100px
   :alt: Crate.IO
   :target: https://crate.io

.. image:: https://travis-ci.org/crate/crate-example-plugin.svg?branch=master
        :target: https://travis-ci.org/crate/crate-example-plugin
        :alt: Build status

======================
 Crate Example Plugin
======================

.. highlight:: sql

This is an example plugin for Crate_ demontrating its plugin
infrastructure. It is implementing a simple ``classnamer()`` scalar function.
Once build, it's JAR file can just be dropped into crate's class path
and after restarting the node/cluster, a shiny new awesome
``classnamer()`` scalar function will be available on all SQL
statements.

Example:

::

   cr> select classnamer() from sys.nodes;
   +------------------------------+
   | classnamer()                 |
   +------------------------------+
   | ScriptableResourceSubscriber |
   | StatefulGraphPreference      |
   +------------------------------+
   SELECT 2 rows in set (0.003 sec)

Or with ``plugin.example.executeScalarPerRow`` setting set to
``false``:

::

   cr> select classnamer() from sys.nodes;
   +------------------------------+
   | classnamer()                 |
   +------------------------------+
   | ThreadedResultReader         |
   | ThreadedResultReader         |
   +------------------------------+
   SELECT 2 rows in set (0.003 sec)

Beside of tests & gradle stuff, this package contains just 3 files:

:META-INF/services/io.crate.Plugin:
   Defines the main plugin class name, crate's plugin loader will
   load. As expected, the file must contain:
   ``io.crate.plugin.ExamplePlugin`` ;).

:io.crate.plugin.ExamplePlugin:
   The main plugin class.

:io.crate.operation.scalar.ClassnamerFunction:
   The scalar function implementation.


Build & Install
===============

In order to use it with an existing crate installation, one must build
a JAR and copy all related JAR's into crate's class path (usually at
``<CRATE_HOME>/lib``).

Build JAR
---------

::

   ./gradlew jar

Build a single JAR including all dependencies
---------------------------------------------

::

   ./gradlew jarIncludingDependencies


Install JAR
-----------

Copy plugin's single JAR to crate's plugins directory::

  cp build/libs/crate-example-plugin.jar <CRATE_HOME>/plugins/

Run tests
=========

All test can be run by a single gradle task::

  ./gradlew test

Help & Contact
==============

Do you have any questions? Or suggestions? We would be very happy
to help you. So, feel free to swing by our IRC channel #crate on Freenode_.
Or for further information and official contact please
visit `https://crate.io/ <https://crate.io/>`_.

.. _Freenode: http://freenode.net

License
=======

Copyright 2013-2015 CRATE Technology GmbH ("Crate")

Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  Crate licenses
this file to you under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.  You may
obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations
under the License.

However, if you have executed another commercial license agreement
with Crate these terms will supersede the license and you may use the
software solely pursuant to the terms of the relevant commercial agreement.

.. _Crate: https://github.com/crate/crate

