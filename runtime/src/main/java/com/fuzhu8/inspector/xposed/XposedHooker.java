package com.fuzhu8.inspector.xposed;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;

import com.fuzhu8.inspector.advisor.AbstractHooker;
import com.fuzhu8.inspector.advisor.Hookable;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * @author zhkl0228
 *
 */
public class XposedHooker extends AbstractHooker<XC_MethodHook> {

	@Override
	public void hook(Class<?> clazz, String method, Hookable handler,
			Class<?>... params) throws NoSuchMethodException {
		Member member;
		if(method == null) {
			member = clazz.getDeclaredConstructor(params);
		} else {
			member = clazz.getDeclaredMethod(method, params);
		}
		
		hook(clazz, member, handler);
	}

	@Override
	public void hook(Class<?> clazz, Member member, Hookable handler) {
		if (member instanceof Constructor) {
			XposedBridge.hookMethod(member, new XposedConstructorHook(handler));
		} else {
			XposedBridge.hookMethod(member, new XposedMethodHook(handler));
		}
	}

	@Override
	protected void log(String msg) {
		XposedBridge.log(msg);
	}

	@Override
	protected void log(Throwable t) {
		XposedBridge.log(t);
	}

	@Override
	public void hookAllConstructors(Class<?> clazz, XC_MethodHook callback) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void hookMethod(Member method, XC_MethodHook callback) {
		XposedBridge.hookMethod(method, callback);
	}

}
