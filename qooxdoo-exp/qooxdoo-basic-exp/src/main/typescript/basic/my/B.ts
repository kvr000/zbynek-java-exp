module basic.my {

import A = require("basic.my.A");


export class B extends basic.my.A
{
	public greet(): String
	{
		return super.greet();
	}

	public getName(): String
	{
		return this.name;
	}
}

}
