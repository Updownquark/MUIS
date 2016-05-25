<?xml version="1.0" encoding="UTF-8"?>

<quick xmlns:base="../../../../base/QuickRegistry.xml" xmlns:test="../../../QuickRegistry.xml">
    <head>
        <title>Testing Buttons</title>
        <model name="model" class="org.quick.test.model.ButtonTestModel" />
    </head>
    <body layout="simple">
    	<block layout="simple" width="100%" top="0" height="100%">
    		Choose a background color:
    		<toggle-button left="10" top="20px" height="30px" value="model.colorGroup.red">Red</toggle-button>
    		<toggle-button left="10" top="50px" height="30px" value="model.colorGroup.blue">Blue</toggle-button>
    		<toggle-button left="10" top="80px" height="30px" value="model.colorGroup.green">Green</toggle-button>
    		<toggle-button left="10" top="110px" height="30px" value="model.colorGroup.purple">Purple</toggle-button>
    		<label value="model.colorGroup" style="font={size=18;weight=bold}" right="10xp" top="60" />
    	</block>
    </body>
</quick>
