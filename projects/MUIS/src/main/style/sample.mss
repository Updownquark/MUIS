
//A sample style sheet for MUIS.  Just trying stuff out here.

button
{
	bg.texture=base:raised-round;
	bg={
		color=blue;
		corner-radius=5;
	}
	.hovered{
		color=yellow;
	}
}
button.clicked{
	bg.color=purple;
}
(group){
	bg.color=green
}
(group)[button, block]{
	.clicked{
		bg.color=orange
	}
}
body(group){
	bg.color=gray
}
