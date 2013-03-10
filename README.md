
# It is free

This is an Android dictionary App. You can visit [Google Play](https://play.google.com/store/apps/details?id=com.baohaojun.crossdictgcide) to
install it, or if your Android phone can't use Google Play, then you
can download it from [Sina Vdisk](http://vdisk.weibo.com/s/skNbH) (need sign up).

It is free software.

The dictionary data that comes with this App is extracted from [GCIDE](ftp://ftp.gnu.org/gnu/gcide/),
which is a GNU project and therefor also free. I have fixed some bugs
in the [dico](http://puszcza.gnu.org.ua/software/dico/) project and used it to extract the dictionary data, and
[here](https://github.com/baohaojun/dico) it is my changes.

But you can switch the dictionary data if you have access to another
one. For example, the American Heritage Dictionary's data can be used,
if it can be changed to CrossDictGcide's data format.

# It is geeky

I have made some features that I think every dictionary should have, for e.g., 

-   Search the definitions
    
    For instance, you can search which words has used the word "hello"
    in their definition. This is why it is named CrossDictGcide.

-   Search with regexp
    
    For instance, you can search with "let$" and found out all those
    small things which ends with "let". This is very useful as it allow
    you check words with similar postfix/prefix/root.

-   Context menu
    
    While in the dictionary, you can long press on a word, and a pop-up
    menu will show, which allows you to check the various things about
    this word. Thank [this project on github](https://github.com/btate/BTAndroidWebViewSelection) for this feature, and if
    you haven't noticed, the CrossDictGcide project is a [fork](https://github.com/btate/BTAndroidWebViewSelection/network) of that one.

# It will be geekier

I am working on these features:

-   Support more devices
    
    Currently only Android 4.0 or above is supported.

-   Support word capture/history better
    
    Using the social sharing feature that is built in Android, another
    App (such as fbreader) can "share" a word with CrossDictGcide, which is
    of course meant to look that word up or capture it for later study.

-   Separate the dictionary data with the App
    
    Or else the App will be too large and difficult to install/upgrade.

# It is done in a geeky way

I used Emacs and no Eclipse for developping this App. In fact, it is
my first big Java project. I haven't wrote any big Java projects
before because I am a huge Emacs fan and can't bear the thought of
switching to Eclipse for Java/Android development.

Then I tried [emacs-eclim](https://github.com/senny/emacs-eclim). It was great, but can't fill my appetite
because I wanted to put the whole Android source into a single
project &ndash; this brought the backend Eclipse to crawling.

But anyway, emacs-eclim has inspired me to work on my own Emacs/Java
solution based on [gtags](http://www.gnu.org/software/global/) and [exuberant ctags](http://ctags.sourceforge.net/).

It's not finished and not ready for public use yet, but you can take
a look of it at [here](http://baohaojun.github.com/coding-android-java-in-emacs-en.html) (video link included).

# Please support

If you used and liked CrossDictGcide and want to support my work on it,
please click [here](http://baohaojun.github.com/donate.html) to donate.
