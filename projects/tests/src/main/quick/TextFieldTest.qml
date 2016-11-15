<?xml version="1.0" encoding="UTF-8"?>

<quick>
    <head>
        <title>Text Field Test</title>
		<style-sheet ref="../styles/quick-tests.qss"></style-sheet>
		<model name="textFieldModel" builder="default-model">
			<variable name="simpleString">&quot;&quot;</variable>
			<variable name="simpleInt">0</variable>
		</model>
    </head>
    <body xmlns:base="../../../../base/QuickRegistry.xml">
   		<block layout="base:box" style="font.family=Arial" direction="down">
   			<text-field length="30" format="${formats.string}" value="textFieldModel.simpleString">This is a text field</text-field>
   			<border>
   				This text is in a normal border
   			</border>
   			<text-field group="tfredborder" length="50">This text field's border should be red</text-field>
   			<text-field multi-line="true" length="100" rows="4">This text field can have multiple lines.
   				Can't put multi-lines in Quick files though.  Gotta be typed by the user currently.
   			</text-field>
   			This text field's value is duplicated on the next line
   			<text-field length="30" format="${formats.string}" value="textFieldModel.simpleString" />
   			<label format="${formats.string}" value="textFieldModel.simpleString" />
   			This one too, but this text field's value can only be an integer
   			<text-field length="30" format="${formats.integer}" value="textFieldModel.simpleInt" />
   			<label format="${formats.integer}" value="textFieldModel.simpleInt" />
   		</block>
    </body>
</quick>
