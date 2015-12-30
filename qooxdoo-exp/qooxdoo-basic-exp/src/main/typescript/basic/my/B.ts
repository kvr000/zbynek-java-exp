module basic.my {

export class B extends A
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
