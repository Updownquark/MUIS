
<quick>
	<head>
		<title>Group Layout Test</title>
	</head>
	<body>
		<block layout="group-layout" direction="down">
			<label align="center">Enter Your Information</label>
			<group align="justify" direction="right">
				<group direction="down">
					<label h-group="name" align="trailing">Name:</label>
					<label h-group="dob" align="trailing">DOB:</label>
					<label h-group="height" align="trailing">Height:</label>
					<label h-group="weight" align="trailing">Weight:</label>
				</group>
				<group direction="down">
					<text-field h-group="name" value="&quot;Updown Quark&quot;" />
					<group h-group="dob" align="justify" direction="right">
						<!--<select values="constants.months" selected="selected.dobMonth" />-->
						<text-field value="&quot;December&quot;" />
						<text-field format="integer" value="21" />
						<text-field format="integer" value="2012" />
						<!--<select value="constants.dobYears" selected="selected.dobYear" />-->
					</group>
					<group h-group="height" align="justify">
						<spinner format="number" value="1.9" />
						<label v-group="unit">m</label>
					</group>
					<group h-group="weight" align="justify">
						<spinner format="number" value="85" />
						<label v-group="unit">kg.</label>
					</group>
				</group>
			</group>
		</block>
	</body>
</quick>
