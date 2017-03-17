package origami.code;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import origami.OEnv;

public interface DynamicInvokable extends OCode {
	public MethodHandle getMethodHandle(OEnv env, MethodHandles.Lookup lookup) throws Throwable;

}