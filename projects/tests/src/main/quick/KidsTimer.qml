<?xml version="1.0" encoding="UTF-8"?>

<quick>
	<head>
		<title>Kids Timer</title>
		<style-sheet ref="../styles/quick-tests.qss"></style-sheet>
		<model name="model" builder="default-model">
			<model name="counter" builder="counter-model" min="0" max="3000" rate="100mi" loop="false" max-frequency="100mi" start="false" />
			<value name="start" type="java.time.Duration">${this.counter.max*100}</value>
			<value name="remaining" type="java.time.Duration">${(this.counter.max-this.counter.value)*100}</value>
		</model>
	</head>
	<body xmlns:base="../../../../base/QuickRegistry.xml" layout="box" direction="down" cross-align="center">
		<spinner value="model.start" format-factory="${formats.durationHHMMss}" rich="true" style="font.size=40" />
		<block layout="box">
			<label value="model.remaining" format-factory="${formats.durationHHMMss}" />
			<label value="(model.counter.max-model.counter.value)%10" />
		</block>
		<block layout="box">
    		<toggle-button selected="model.counter.running">
    			<label value="model.counter.running ? &quot;Pause&quot; : (model.counter.value==model.counter.min ? &quot;Start&quot; : &quot;Resume&quot;)" />
    		</toggle-button>
			<button action="model.counter.value=model.counter.min;model.counter.running=false">Stop</button>
		</block>
		<!-- TODO The Stop button should make the counter stop running -->
	</body>
</quick>
