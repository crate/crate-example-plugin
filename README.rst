=======================
 CrateDB Example Plugin
=======================

.. image:: https://travis-ci.org/crate/crate-example-plugin.svg?branch=master
        :target: https://travis-ci.org/crate/crate-example-plugin
        :alt: Build status

|

This is an example plugin for demonstrating the CrateDB plugin
infrastructure.

This plugin implements a simple ``classnamer()`` scalar function.

Once built, the plugin JAR file can be dropped into CrateDB's plugin path. After
restarting the CrateDB node or cluster, the ``classnamer()`` scalar function
will be available for all SQL statements.

Examples
========

Here's a simple example:

::

   cr> select classnamer() from sys.nodes;
   +------------------------------+
   | classnamer()                 |
   +------------------------------+
   | ScriptableResourceSubscriber |
   | StatefulGraphPreference      |
   +------------------------------+
   SELECT 2 rows in set (0.003 sec)

Or with the ``plugin.example.executeScalarPerRow`` setting set to ``false``::

   cr> select classnamer() from sys.nodes;
   +------------------------------+
   | classnamer()                 |
   +------------------------------+
   | ThreadedResultReader         |
   | ThreadedResultReader         |
   +------------------------------+
   SELECT 2 rows in set (0.003 sec)

File Layout
===========

Beside of tests and build scaffolding, this package contains just three files:

- `META-INF/services/io.crate.Plugin`_
    Defines the main plugin class name.

- `io.crate.plugin.ExamplePlugin`_
    The main plugin class.

- `io.crate.operation.scalar.ClassnamerFunction`_
    The scalar function implementation.


Building and Installing
=====================

To use this plugin with an existing CrateDB installation, you must build
a JAR file and copy all related JARs into CrateDB's class path (usually at
``<CRATE_HOME>/lib``).

Build the JAR file like so::

    $ ./gradlew jar

Then copy JAR file to CrateDB's plugins directory::

    $ cp build/libs/crate-example-plugin.jar <CRATE_HOME>/plugins/

Here, ``CRATE_HOME`` is the root of your CrateDB installation.

Running Tests
=============

You can run the tests like so::

    $ ./gradlew test

Help
====

Looking for more help?

- Check `StackOverflow`_ for common problems
- Chat with us on `Slack`_
- Get `paid support`_


.. _CrateDB: https://github.com/crate/crate
.. _io.crate.operation.scalar.ClassnamerFunction: src/main/java/io/crate/operation/scalar/ClassnamerFunction.java
.. _io.crate.plugin.ExamplePlugin: src/main/java/io/crate/plugin/ExamplePlugin.java
.. _META-INF/services/io.crate.Plugin: https://github.com/crate/crate-example-plugin/blob/nomi/top-level-docs/src/main/resources/META-INF/services/io.crate.Plugin
.. _paid support: https://crate.io/pricing/
.. _Slack: https://crate.io/docs/support/slackin/
.. _StackOverflow: https://stackoverflow.com/tags/crate
