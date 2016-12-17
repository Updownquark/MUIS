<?xml version="1.0" encoding="UTF-8"?>

<quick>
    <head>
        <title>Counter Test</title>
        <model name="counter" builder="counter-model" init="0" max="3" rate="10s" step="1" max-frequency="100mi" />
        <model name="counterValues" builder="default-model">
        	<switch name="red" value="Math.floor(counter.value)">
        		<case value="0">counter.value*255</case>
        		<case value="1">255*(2-counter.value)</case>
        		<default>0</default>
        	</switch>
        	<switch name="green" value="Math.floor(counter.value)">
        		<case value="1">(counter.value-1)*255</case>
        		<case value="2">255*(3-counter.value)</case>
        		<default>0</default>
        	</switch>
        	<switch name="green" value="Math.floor(counter.value)">
        		<case value="2">(counter.value-2)*255</case>
        		<case value="0">255*(1-counter.value)</case>
        		<default>0</default>
        	</switch>
        </model>
    </head>
    <body layout="base:simple" xmlns:base="../../../../base/QuickRegistry.xml">
    	<label left="10" top="10" value="counter.value" />
    	<base:block left="0" right="0xp" top="50" bottom="0xp"
    		style="bg.color=rgb(counterValues.red, counterValues.green, counterValues.blue" />
    </body>
</quick>
