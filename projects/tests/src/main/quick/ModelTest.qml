<?xml version="1.0" encoding="UTF-8"?>

<quick xmlns:test="../../../QuickRegistry.xml">
    <head>
        <title>Model Test</title>
		<style-sheet ref="../styles/quick-tests.qss"></style-sheet>
        <model name="model" builder="default-model">
        	<variable name="colorIndex">0</variable>
        	<switch name="bg" type="color" value="this.colorIndex">
        		<case value="0">red</case>
        		<case value="1">blue</case>
        		<case value="2">green</case>
        	</switch>
        	<switch name="fg" type="color" value="this.colorIndex">
        		<case value="0">black</case>
        		<case value="1">white</case>
        		<case value="2">black</case>
        	</switch>
        	<switch name="bgImage" type="resource" value="this.colorIndex">
        		<case value="0">test:fire</case>
        		<case value="1">test:waterfall</case>
        		<case value="2">test:plant</case>
        	</switch>
        	<switch name="group" type="string" value="this.colorIndex">
        		<case value="0">bg-red</case>
        		<case value="1">bg-blue</case>
        		<case value="2">bg-green</case>
        	</switch>
        </model>
    </head>
    <body xmlns:base="../../../../base/QuickRegistry.xml">
   		<block layout="base:border-layout">
   			<label region="top" height="30" style="font.size=24">Model Test</label>
   			<block region="left" width="120" layout="box" direction="down" cross-align="justify">
   				<label style="font.size=16">Choose Background</label>
	    		<toggle-button selected="model.colorIndex==0">Red</toggle-button>
	    		<toggle-button selected="model.colorIndex==1">Blue</toggle-button>
	    		<toggle-button selected="model.colorIndex==2">Green</toggle-button>
	    		<image src="${model.bgImage}" prop-locked="true" resize="resize" width="100%"/>
   			</block>
   			<block region="right" width="200" layout="box" direction="down">
	   			<text-field value="model.bg" format="${formats.color}" length="30"></text-field>
   			</block>
   			<border region="center" group="model.group" style="bg.transparency=0;bg.color=${model.bg};font.color=${model.fg}">
   				This text's background should match the selected button at left.
   				Its font color should match the value at right.
   			</border>
   		</block>
    </body>
</quick>
