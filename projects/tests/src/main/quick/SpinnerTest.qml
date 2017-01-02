<?xml version="1.0" encoding="UTF-8"?>

<quick>
    <head>
        <title>Spinner Test</title>
		<style-sheet ref="../styles/quick-tests.qss"></style-sheet>
		<model name="model" builder="default-model">
			<variable name="simpleInt" min="0" max="100000">0</variable>
			<variable name="simpleFloat" min="-1000" max="1000">0.0</variable>
			<variable name="simpleDuration" type="java.time.Duration">0m</variable>
		</model>
	</head>
	<body xmlns:base="../../../../base/QuickRegistry.xml" layout="box" direction="down">
		<block layout="box" direction="right">
			<spinner value="model.simpleInt" format-factory="${formats.advancedInteger}" />
			This spinner increments an integer by a power of ten, depending on the location of the cursor.
		</block>
		<block layout="box" direction="right">
			<spinner value="model.simpleInt" format="${formats.integer}" increment="model.simpleInt+=10" decrement="model.simpleInt-=10" />
			This spinner increments the same integer by 10.
		</block>
		<block layout="box" direction="right">
			<spinner value="model.simpleFloat" format="${formats.number}" increment="model.simpleFloat++" decrement="model.simpleFloat--" />
			This spinner increments a floating-point value by 1.
		</block>
		<block layout="box" direction="right">
			<spinner value="model.simpleFloat" format="${formats.number}" increment="model.simpleFloat+=100" decrement="model.simpleFloat-=100" />
			This spinner increments the same floating-point value by 100.
		</block>
		<block layout="box" direction="right">
			<spinner value="model.simpleDuration" format-factory="${formats.durationHHMMss}" rich="true" style="font.weight=bold" />
			This spinner operates on a duration, incrementing by hours, minutes, or seconds depending on the location of the cursor.
		</block>
	</body>
</quick>