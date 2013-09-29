package com.secpro.platform.monitoring.agent.test.job;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class TimezoneTester {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		long val = 1 - 4;
		System.out.println(Math.abs(val));
		File aFile = new File("/home/baiyanwei/secpro/project/run/task1/taskCache.list");
		if (aFile.exists() == false) {
			File parentFile = aFile.getParentFile();
			if (parentFile.exists() == false) {
				parentFile.mkdirs();
			}
			try {
				aFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		ArrayList<String> aList = new ArrayList<String>();

		aList.add("1");
		aList.add("2");
		aList.add("3");
		aList.add("4");
		aList.add("5");

		for (String str : aList) {
			System.out.println(str);
		}
		Iterator itr = aList.iterator();

		// remove 2 from ArrayList using Iterator's remove method.

		String strElement = "";

		while (itr.hasNext()) {
			strElement = (String) itr.next();
			if (strElement.equals("2")) {
				System.out.println("remove element:" + strElement);
				itr.remove();
				// break;
			}
			if (strElement.equals("4")) {
				System.out.println("remove element:" + strElement);
				itr.remove();
				// break;
			}
		}
		for (String str : aList) {
			System.out.println(str);
		}
	}

}
