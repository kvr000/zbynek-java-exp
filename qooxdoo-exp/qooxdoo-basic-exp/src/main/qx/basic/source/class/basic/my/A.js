qx.Class.define("basic.my.A", {
	extend: qx.core.Object,

	construct: function () {
		this.name = "xy";
	},

	members: {
		greet: function () {
			alert("Hello " + this.getName());
		},

		getName: function() {
			return this.name;
		}
	}
});
