<?xml version="1.0" encoding="UTF-8"?>

<quick>
    <head>
        <title>Counter Test</title>
        <model name="model" builder="default-model">
	        <model name="counter" builder="counter-model" init="0" max="300" rate="100mi" max-frequency="100mi" />
	        <value name="value">this.counter.value/100f</value>
        	<switch name="red" value="(int)Math.floor(this.value)">
        		<case value="0">this.value*255</case>
        		<case value="1">255*(2-this.value)</case>
        		<default>0</default>
        	</switch>
        	<switch name="green" value="(int)Math.floor(this.value)">
        		<case value="1">(this.value-1)*255</case>
        		<case value="2">255*(3-this.value)</case>
        		<default>0</default>
        	</switch>
        	<switch name="blue" value="(int)Math.floor(this.value)">
        		<case value="2">(this.value-2)*255</case>
        		<case value="0">255*(1-this.value)</case>
        		<default>0</default>
        	</switch>
        </model>
    </head>
    <body xmlns:base="../../../../base/QuickRegistry.xml" layout="simple">
    	<block top="10" layout="box">
    	<label value="model.value" />
    	Red: <label value="model.red" />
    	Green: <label value="model.green" />
    	Blue: <label value="model.blue" />
    	</block>
    	<block left="0" right="0xp" top="50" bottom="0xp"
    		style="bg.transparency=0;bg.color=${rgb((int) model.red, (int) model.green, (int) model.blue)}" />
    </body>
</quick>
