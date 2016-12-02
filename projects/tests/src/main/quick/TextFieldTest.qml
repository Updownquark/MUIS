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
   			<text-field length="30" format="${formats.string}" value="textFieldModel.simpleString" />
   			<border>
   				This text is in a normal border
   			</border>
   			<text-field group="tfredborder" length="50" value="&quot;This text field's border should be red&quot;" />
   			This text field's value is duplicated on the next line and in the top text field
   			<text-field length="30" format="${formats.string}" value="textFieldModel.simpleString" />
   			<label format="${formats.string}" value="textFieldModel.simpleString" />
   			This field's value is on the next line too, but this one can only be an integer
   			<text-field length="30" format="${formats.integer}" value="textFieldModel.simpleInt" />
   			<label format="${formats.integer}" value="textFieldModel.simpleInt" />
   		</block>
    </body>
</quick>
