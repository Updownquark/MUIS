<?xml version="1.0" encoding="UTF-8"?>

<quick>
    <head>
        <title>Counter Test</title>
        <model name="model" builder="default-model">
	        <model name="counter" builder="counter-model" init="0" max="3" rate="10s" step="0.01" max-frequency="100mi" />
        	<switch name="red" value="Math.floor(this.counter.value)">
        		<case value="0">this.counter.value*255</case>
        		<case value="1">255*(2-this.counter.value)</case>
        		<default>0</default>
        	</switch>
        	<switch name="green" value="Math.floor(this.counter.value)">
        		<case value="1">(this.counter.value-1)*255</case>
        		<case value="2">255*(3-this.counter.value)</case>
        		<default>0</default>
        	</switch>
        	<switch name="blue" value="Math.floor(this.counter.value)">
        		<case value="2">(this.counter.value-2)*255</case>
        		<case value="0">255*(1-this.counter.value)</case>
        		<default>0</default>
        	</switch>
        </model>
    </head>
    <body layout="base:simple" xmlns:base="../../../../base/QuickRegistry.xml">
    	<label left="10" top="10" value="model.counter.value" />
    	<base:block left="0" right="0xp" top="50" bottom="0xp"
    		style="bg.color=${rgb((int) model.red, (int) model.green, (int) model.blue)}" />
    </body>
</quick>
