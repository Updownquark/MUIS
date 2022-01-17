Quick
====

Quality User Interface Creation Kit

This project is deprecated.  I've done a hard pivot on this idea, pulling the core concepts into the Qonfig and Quick APIs in the Qommons and ObServe projects, respectively.  I may refer back to this document and this project for ideas on how to make those APIs better, but the entire implementation of this is abandoned.

My user inteface architecture

(Copied from the old repository description at https://code.google.com/p/webapplicationmarkup/, so may not be up-to-date and may call itself WAM or MUIS occasionally)

The goal of the Quick project is to replace HTML as the most practical means of creating and distributing full-featured web applications. It aims to be a DOM-based, java-based language that allows developers to easily write applications viewable in a browser without the headache of the inherent language deficiencies of HTML/CSS/javascript and without browser dependencies.

Then the body section will function similar to that of HTML. It will contain tag names that are interpreted (via the class mapping entries) as widgets. The widgets will render their portion of the graphics display and interpret events passed to them by the system.

Quick is designed to integrate the best parts of current web development languages with the best parts of thick client development.

The advantages of web development which Quick attempts to incorporate are
 * Remote access: applications need not be installed or deployed because everything needed to run them is in a pre-installed browser and the server's application definition.
 * A DOM-tree. The DOM nature of HTML allows the developer to easily see the way graphical components are laid out and to copy graphical sections from one area to another easily.
 * Styles. CSS classes allow large and complex applications to be transformed easily. Quick styles will accomplish this function but with larger flexibility.
 * Security. Untrusted web pages present a much smaller security risk than untrusted thick client installation and execution due to restrictions on operations that can be performed from code.

Thick client development has numerous advantages over traditional web programming, such as
 * Strongly typed language. Strong typing is a powerful tool to remove bugs before they are introduced into the code base. Strongly typed languages also have more powerful tools for development thanks to their well-formed nature and referential integrity. Java is arguably the language with the most powerful development tools in the world, is available cross-platform, and is itself an extremely flexible and powerful language, so it was chosen for the basis of the Quick language.
Easier Graphical User Interface (GUI) development. Although HTML is well suited to simple presentation of organized data, the rules for layout of thick client applications are typically more predictable. Also, due to this predictability and the lack of browser-dependencies, WYSIWYG GUI editors are easier to implement with thick clients. The graphical power of thick clients are also vastly superior because the widgets have access to pixel-by-pixel graphics displays.
Distribution of computation. Due to the difficulties of developing business logic in javascript and the expense of proprietary client-side languages, web applications generally run the majority of computational work on the server, only transferring display information to the client. Having Java code run on the client will allow the client to run resource-intensive tasks, making the server a simple data source or web service. This model will contribute greatly to the types of web applications that can be developed.
 * More responsive. An added benefit of having more program execution run on the client is that the GUI's state can be updated more quickly in response to user events, since minimal or no network communication needs to occur.
HTML was developed as a presentation language for simple data sets. It has been improved significantly many times since its original form to include scripting and styles, which has allowed greater varieties of data to be presented. However, different browsers have arisen independently which interpret the languages with slight differences. In addition, the number of widgets quickly available to the developer is limited. More complex widgets such as spinners and trees and complex operations such as dragging are extremely difficult to correctly implement in a single implementation of the web environment, much more to support multiple environments well. While HTML is still sufficient for simpler presentations, development of complicated web applications is significantly impacted.

Many diverse and useful toolkits have been developed to ease web development. The one thing that all of these have in common is that the final product of the toolkit is a combination of HTML, javascript, and CSS. The ability of a toolkit to provide a large set of functionality and render correctly in the different environments is limited by the functionality of these technologies as interpreted by the different browsers. While many toolkits have made significant progress in this area, the development is significantly hindered by the base technology. This situation is not acceptable for the future of web application development.

While DHTML(HTML/CSS/javascript) is the most common basis for web applications, other technologies exist which provide significant advantages. Examples include Adobe Flash and ActiveX. However, these also have disadvantages that Quick attempts to avoid. The first is that one generally has to choose between popular technologies supported by many browsers and pre-installed by many users, and proprietary technologies which may be extremely expensive not only to develop, but to maintain for the life of the application. The former is prohibitive to all prospective developers seeking to have their applications accessed by many different classes of users. The latter is a disadvantage to all developers, but especially to smaller development enterprises with limited financial resources. A third disadvantage is that such mobile code presents a security problem. Allowing the programs to execute on the client's computer could potentially allow it to perform unsafe operations or operations that interfere with the the client's normal behavior.

Quick will solve many of these problems by providing a free, open-source solution that allows quick and easy development of simple web applications as well as extremely powerful rich internet applications.

Optimized using JProfiler (http://www.ej-technologies.com/products/jprofiler/overview.html)
