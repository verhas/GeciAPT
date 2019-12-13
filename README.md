# Experimental Tool to Export Java Compiler AST into XML

This application is an annotation processor. Put in on the compilation
classpath and it will do its work. If you use Maven then all you need
to do is add a dependency

```xml
<dependency>
    <groupId>com.javax0.geci</groupId>
    <artifactId>geciapt</artifactId>
    <version>1.0.0</version>
    <scope>compile</scope>
</dependency>
``` 

It will generate an XML file in the same directory where the `.class`
files are generated containing the Abstract Syntax Tree of the
individual files.

You can open the XML files to see how the AST structure looks like or
you can create a tool that analyses the generated XML structures using
the well known XML APIs using XPath whatnot. You can also have a look at
the source to learn the not too well documented AST structure.

This project is currently experimental, there is no compatibility
promise for the structure of the XML in later versions, or that there
will be later versions or even a release version at all.

Any suggestion or comment is more than welcome.