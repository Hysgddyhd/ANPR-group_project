package main;

public class example {
	public static void main(String args[]) {
		int a = 0;
		for (;a<10;a++) {
			System.out.println(a);
			if(a==6) {
				return;
			}
		}
	}
}
