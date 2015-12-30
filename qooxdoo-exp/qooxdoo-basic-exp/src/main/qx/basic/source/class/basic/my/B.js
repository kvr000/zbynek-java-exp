qx.Class.define("basic.my.B", {
	extend: basic.my.A,
	implement: basic.my.Greetable,
	construct: function (x) {
		this.base(arguments, x);
	},
	members: {
		greet: function () {
			this.base(arguments);
		},
		getName: function() {
			return this.name;
		}
	}
});
