<?xml version="1.0" encoding="UTF-8"?>

<quick xmlns:base="../../../../base/QuickRegistry.xml" xmlns:test="../../../QuickRegistry.xml">
    <head>
        <title>Testing Buttons</title>
        <model name="model" builder="org.quick.test.model.ButtonTestModel$Builder" />
        <model name="model">
        	<variable name="value">0</variable>
        	<switch name="colorGroup" value="this.value%4">
        		<case from="0">#bg-red</case>
        		<case from="1">#bg-blue</case>
        		<case from="2">#bg-green</case>
        		<case from="3">#bg-purple</case>
        	</switch>
        	<value name="red">this.value==0</value>
        	<value name="blue">this.value==1</value>
        	<value name="green">this.value==2</value>
        	<value name="purple">this.value==3</value>
        </model>
    </head>
    <body layout="simple">
    	<block layout="simple" width="100%" top="0" height="100%">
    		Choose a background color:
    		<toggle-button left="10" top="20px" height="30px" value="model.red">Red</toggle-button>
    		<toggle-button left="10" top="50px" height="30px" value="model.blue">Blue</toggle-button>
    		<toggle-button left="10" top="80px" height="30px" value="model.green">Green</toggle-button>
    		<toggle-button left="10" top="110px" height="30px" value="model.purple">Purple</toggle-button>
    		<label value="model.colorGroup" style="font.size=18;font.weight=bold" right="10xp" top="60" />
    	</block>
    </body>
</quick>
