package jp.skypencil.kemuri;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.DUP2;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.INEG;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IXOR;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SWAP;
import static org.objectweb.asm.Opcodes.V1_5;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class Compiler {
	private final Logger logger = LoggerFactory.getLogger(Compiler.class);

	private static final Type[] DEQUE_TYPE = new Type[] { Type.getType(Deque.class) };
	private static final List<Character> HELLO_WORLD = Lists.reverse(Arrays.asList(new Character[] {
			72, 101, 108, 108, 111, 44, 32, 119, 111, 114, 108, 100, 33
	}));
	private static final Type[] EMPTY_TYPE = new Type[0];

	private void compile(MethodVisitor mv, Reader reader, String innerFullClassName) throws IOException {
		int command;
		while ((command = reader.read()) != -1) {
			exec(mv, command, innerFullClassName);
		}
	}

	public byte[] compile(Reader reader, String classFullName) throws IOException {
		checkNotNull(reader);
		String className = checkNotNull(classFullName);
		String innerFullClassName = classFullName.replaceAll("\\.", "/");

		if (className.contains(".")) {
			className = className.substring(className.lastIndexOf('.') + 1);
		}

		ClassWriter cw = new ClassWriter(0);
		cw.visit(V1_5, ACC_PUBLIC, innerFullClassName, null, "java/lang/Object", null);
		createConstructor(cw, innerFullClassName);
		createDup(cw, innerFullClassName);
		createPrint(cw, innerFullClassName);
		createHello(cw, innerFullClassName);
		createNot(cw, innerFullClassName);
		createRot(cw, innerFullClassName);
		createXor(cw, innerFullClassName);
		createMain(cw, reader, innerFullClassName);
		cw.visitEnd();

		return cw.toByteArray();
	}

	public void compileTo(Reader reader, String className, File directory) throws IOException {
		checkNotNull(reader);
		checkNotNull(className);
		checkNotNull(directory);
		checkArgument(directory.isDirectory());

		File classFile = new File(directory, className.concat(".class"));
		byte[] binary = compile(reader, className);
		Files.write(binary, classFile);
	}

	private void createConstructor(ClassWriter cw, String innerFullClassName) {
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitMaxs(1, 1);
		mv.visitVarInsn(ALOAD, 0); // push `this` to the operand stack
		mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V"); // call the constructor of super class

		mv.visitInsn(RETURN);
		mv.visitEnd();
	}

	private void createDup(ClassWriter cw, String innerFullClassName) {
		MethodVisitor mv = cw.visitMethod(
				ACC_STATIC,
				"dup",
				Type.getMethodDescriptor(Type.VOID_TYPE, DEQUE_TYPE),
				null, null);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Deque.class), "pop", Type.getMethodDescriptor(Type.getType(Object.class), EMPTY_TYPE));
		mv.visitInsn(DUP2);
		mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Deque.class), "push", Type.getMethodDescriptor(Type.VOID_TYPE, new Type[]{ Type.getType(Object.class) }));
		mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Deque.class), "push", Type.getMethodDescriptor(Type.VOID_TYPE, new Type[]{ Type.getType(Object.class) }));
		mv.visitInsn(RETURN);

		mv.visitMaxs(4, 1);
		mv.visitEnd();
	}

	private void createHello(ClassWriter cw, String innerFullClassName) {
		MethodVisitor mv = cw.visitMethod(
				ACC_STATIC,
				"hello",
				Type.getMethodDescriptor(Type.VOID_TYPE, DEQUE_TYPE),
				null, null);
		mv.visitVarInsn(ALOAD, 0);
		for (char c : HELLO_WORLD) {
			mv.visitInsn(DUP);
			mv.visitLdcInsn(c);
			mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Character.class), "valueOf", Type.getMethodDescriptor(Type.getType(Character.class), new Type[]{ Type.getType(char.class) }));
			mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Deque.class), "push", Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { Type.getType(Object.class) }));
		}
		mv.visitInsn(RETURN);

		mv.visitMaxs(3, 1);
		mv.visitEnd();
	}

	private void createMain(ClassWriter cw, Reader reader, String innerFullClassName) throws IOException {
		MethodVisitor mv = cw.visitMethod(
				ACC_PUBLIC | ACC_STATIC,
				"main",
				Type.getMethodDescriptor(Type.VOID_TYPE, new Type[]{Type.getObjectType("[Ljava/lang/String;")}),
				null, null);
		mv.visitTypeInsn(NEW, Type.getInternalName(ArrayDeque.class));
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ArrayDeque.class), "<init>", "()V");

		compile(mv, reader, innerFullClassName);

		mv.visitInsn(POP);
		mv.visitInsn(RETURN);
		mv.visitMaxs(2, 1);
		mv.visitEnd();
	}

	private void createNot(ClassWriter cw, String innerFullClassName) {
		MethodVisitor mv = cw.visitMethod(
				ACC_STATIC,
				"not",
				Type.getMethodDescriptor(Type.VOID_TYPE, DEQUE_TYPE),
				null, null);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Deque.class), "pop", Type.getMethodDescriptor(Type.getType(Object.class), EMPTY_TYPE));
		mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Character.class));
		mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Character.class), "charValue", Type.getMethodDescriptor(Type.getType(char.class), EMPTY_TYPE));
		mv.visitInsn(INEG);
		mv.visitLdcInsn(255);
		mv.visitInsn(IADD);
		mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Character.class), "valueOf", Type.getMethodDescriptor(Type.getType(Character.class), new Type[]{ Type.getType(char.class) }));
		mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Deque.class), "push", Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { Type.getType(Object.class) }));
		mv.visitInsn(RETURN);

		mv.visitMaxs(3, 1);
		mv.visitEnd();
	}

	private void createPrint(ClassWriter cw, String innerFullClassName) {
		MethodVisitor mv = cw.visitMethod(
				ACC_STATIC,
				"print",
				Type.getMethodDescriptor(Type.VOID_TYPE, DEQUE_TYPE),
				null, null);
		Label loopStart = new Label();
		Label loopEnd = new Label();
		mv.visitLabel(loopStart);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Deque.class), "isEmpty", Type.getMethodDescriptor(Type.getType(boolean.class), EMPTY_TYPE));
		mv.visitJumpInsn(IFNE, loopEnd);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Deque.class), "pop", Type.getMethodDescriptor(Type.getType(Object.class), EMPTY_TYPE));
		mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Character.class));
		mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Character.class), "charValue", Type.getMethodDescriptor(Type.getType(char.class), EMPTY_TYPE));
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(C)V");
		mv.visitJumpInsn(GOTO, loopStart);
		mv.visitLabel(loopEnd);
		mv.visitInsn(RETURN);

		mv.visitMaxs(2, 1);
		mv.visitEnd();
	}

	private static final int[] ROT_MAPPING = {1, 3, 2};
	private void createRot(ClassWriter cw, String innerFullClassName) {
		MethodVisitor mv = cw.visitMethod(
				ACC_STATIC,
				"rot",
				Type.getMethodDescriptor(Type.VOID_TYPE, DEQUE_TYPE),
				null, null);
		mv.visitVarInsn(ALOAD, 0);
		for (int i = 0; i < ROT_MAPPING.length; ++i) {
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Deque.class), "pop", Type.getMethodDescriptor(Type.getType(Object.class), EMPTY_TYPE));
			mv.visitVarInsn(ASTORE, i + 1);
		}
		for (int i = 0; i < ROT_MAPPING.length; ++i) {
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, ROT_MAPPING[i]);
			mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Deque.class), "push", Type.getMethodDescriptor(Type.VOID_TYPE, new Type[]{ Type.getType(Object.class) }));
		}
		mv.visitInsn(POP);
		mv.visitInsn(RETURN);

		mv.visitMaxs(3, 4);
		mv.visitEnd();
	}

	private void createXor(ClassWriter cw, String innerFullClassName) {
		MethodVisitor mv = cw.visitMethod(
				ACC_STATIC,
				"xor",
				Type.getMethodDescriptor(Type.VOID_TYPE, DEQUE_TYPE),
				null, null);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitInsn(DUP);
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Deque.class), "pop", Type.getMethodDescriptor(Type.getType(Object.class), EMPTY_TYPE));
		mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Character.class));
		mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Character.class), "charValue", Type.getMethodDescriptor(Type.getType(char.class), EMPTY_TYPE));
		mv.visitInsn(SWAP);
		mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Deque.class), "pop", Type.getMethodDescriptor(Type.getType(Object.class), EMPTY_TYPE));
		mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Character.class));
		mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Character.class), "charValue", Type.getMethodDescriptor(Type.getType(char.class), EMPTY_TYPE));
		mv.visitInsn(IXOR);
		mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Character.class), "valueOf", Type.getMethodDescriptor(Type.getType(Character.class), new Type[]{ Type.getType(char.class) }));
		mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Deque.class), "push", Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { Type.getType(Object.class) }));
		mv.visitInsn(RETURN);

		mv.visitMaxs(3, 1);
		mv.visitEnd();
	}

	private void exec(MethodVisitor mv, int command, String innerFullClassName) {
		mv.visitInsn(DUP);
		switch (command) {
		case '^':
			mv.visitMethodInsn(INVOKESTATIC, innerFullClassName, "xor", Type.getMethodDescriptor(Type.VOID_TYPE, DEQUE_TYPE));
			break;
		case '~':
			mv.visitMethodInsn(INVOKESTATIC, innerFullClassName, "not", Type.getMethodDescriptor(Type.VOID_TYPE, DEQUE_TYPE));
			break;
		case '"':
			mv.visitMethodInsn(INVOKESTATIC, innerFullClassName, "dup", Type.getMethodDescriptor(Type.VOID_TYPE, DEQUE_TYPE));
			break;
		case '\'':
			mv.visitMethodInsn(INVOKESTATIC, innerFullClassName, "rot", Type.getMethodDescriptor(Type.VOID_TYPE, DEQUE_TYPE));
			break;
		case '`':
			mv.visitMethodInsn(INVOKESTATIC, innerFullClassName, "hello", Type.getMethodDescriptor(Type.VOID_TYPE, DEQUE_TYPE));
			break;
		case '|':
			mv.visitMethodInsn(INVOKESTATIC, innerFullClassName, "print", Type.getMethodDescriptor(Type.VOID_TYPE, DEQUE_TYPE));
			break;
		default: 
			logger.warn("unknown command: {}", Character.toString((char) command));
			mv.visitInsn(POP);
		}
	}

}
