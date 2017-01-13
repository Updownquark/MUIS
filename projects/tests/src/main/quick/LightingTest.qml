<?xml version="1.0" encoding="UTF-8"?>

<quick>
    <head>
        <title>Light Circling Test</title>
        <model name="counter" builder="counter-model" min="0" max="360" rate="20mi" max-frequency="100mi" on-finish="loop" />
        <model name="model" builder="default-model">
	        <variable name="value">0</variable>
        </model>
    </head>
    <body xmlns:base="../../../../base/QuickRegistry.xml" layout="box" direction="down" align="justify">
    	<button action="model.value++" style="bg.color=white;
    		light.color=red;
    		light.shadow=blue;
    		light.max-amount=.8;
    		light.source=counter.value">Do Nothing</button>
    </body>
</quick>
