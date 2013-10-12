Digapidoc [![Build Status](https://drone.io/github.com/anvie/digapidoc/status.png)](https://drone.io/github.com/anvie/digapidoc/latest)
=============

A Simple rest API documentation generator. Digapidoc is just parse source code that contains comment 
with Digapidoc format and generate it to static html. 

Digapidoc don't rule your code just rule your comment, whatever your code is. 

If you need more advanced API documentation generator you may try http://apiblueprint.org/

Example
-----------

In your code add the following comment:

```
/**
 * GET /post/{POST-ID}
 *
 * Get single post data.
 * 
 * + Symbols:
 *
 *     + {POST-ID} - ID of a post.
 *
 * + Parameters:
 *
 *     + viewer_user_id - ID of user who's view the post.
 */
```

And run the digapidoc. You need to create your config first.

For more example see [test/source](https://github.com/anvie/digapidoc/tree/master/test/source/src/com/org) dir.

Configuration
--------------------

Example of config file:


```
title = "My cool API v2"

desc = "This is my cool api documentation"

# the dir contains source code where digapidoc should scan.
source-dir = ../digaku-restapi/v2/src/

# the output dir where generated static html will be located.
output-dir = ./out

# include dir, when you need to using `include` in your code
# so you don't need to write and write the same text again.
include-dir = ./include

# The template html dir where Digapidoc will be use
template-dir = ./web-templates/twbs

# include tex, just like `include-dir` but from text, not file.
include-text {
    offset-limit = "+ offset=`0` - starting offset.\n+ limit=`10` - ends limit"
}

# symbol list, so Digapidoc know what it is and wrote the correct symbol info
# in generated documentation, even you didn't specify in `+ Symbols` section.
symbols {
    CHANNEL-ID-OR-NAME = "Can be channel ID or channel name"
    USER-ID-OR-NAME = "Can be user ID or user name"
    WHISPER-ID = "ID of whisper"
}

```

You can run digapidoc like this:

```
$ java -jar digapidoc.jar myapidoc.conf
```

Open your generated html using your browser, for this example your html will be placed in `./out/index.html`.

Or you can see the online output example here: [http://anvie.github.io/digapidoc](http://anvie.github.io/digapidoc/index.html)


Download
------------

You can download Digapidoc from https://github.com/anvie/digapidoc/releases




