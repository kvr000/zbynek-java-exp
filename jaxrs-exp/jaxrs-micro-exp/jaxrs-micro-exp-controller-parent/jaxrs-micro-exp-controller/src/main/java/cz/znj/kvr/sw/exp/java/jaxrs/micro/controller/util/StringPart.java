package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.util;

import lombok.Getter;


/**
 *
 */
public final class StringPart implements CharSequence
{
	public static final StringPart EMPTY = new StringPart(new char[0]);

	public StringPart(String backing)
	{
		this(backing.toCharArray(), 0, backing.length());
	}

	public StringPart(char[] backing)
	{
		this(backing, 0, backing.length);
	}

	public StringPart(char[] backing, int offset, int length)
	{
		this.backing = backing;
		this.offset = offset;
		this.length = length;
		int h = 1;
		for (int i = 0; i < this.length; ++i) {
			h = h*31+this.backing[this.offset+i];
		}
		this.hashCode = h;
	}

	@Override
	public String toString()
	{
		return new String(backing, offset, length);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof StringPart))
			return false;
		StringPart s = (StringPart)obj;
		if (this == s)
			return true;
		if (this.length != s.length)
			return false;
		for (int i = 0; i < this.length; ++i) {
			if (this.backing[this.offset+i] != s.backing[s.offset+i])
				return false;
		}
		return true;
	}

	@Override
	public int hashCode()
	{
		return this.hashCode;
	}

	public int length()
	{
		return length;
	}

	@Override
	public char charAt(int index)
	{
		return this.backing[this.offset+index];
	}

	@Override
	public CharSequence subSequence(int start, int end)
	{
		return subpart(start, end);
	}

	public int indexOf(char c)
	{
		return indexOf(c, 0);
	}

	public int indexOf(char c, int offset)
	{
		for (int i = offset; i < length; ++i) {
			if (backing[this.offset+i] == c)
				return i;
		}
		return -1;
	}

	public StringPart subpart(int start, int end)
	{
		return new StringPart(backing, this.offset+start, end-start);
	}

	public StringPart subpart(int start)
	{
		return new StringPart(backing, this.offset+start, this.length-start);
	}

	private char[] backing;

	@Getter
	private int offset;

	private int length;

	private int hashCode;
}
