export module basic.my {

export class A
{
	public greet(): String
	{
		return "Hello "+this.getName();
	}

	public getName(): String
	{
		return this.name;
	}

	protected name = "xy";
}

}
