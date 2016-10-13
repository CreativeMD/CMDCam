package com.creativemd.cmdcam.transform;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.RETURN;

import java.util.Iterator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;

public class CamTransformer implements IClassTransformer {
	
	public static final String[] names = new String[]{".", "net.minecraft.client.renderer.EntityRenderer", "getMouseOver"};
	public static final String[] namesOb = new String[]{"/", "blt", "a"};
	
	public static String patch(String input)
	{
		for(int zahl = 0; zahl < names.length; zahl++)
			input = input.replace(names[zahl], namesOb[zahl]);
		return input;
	}
	
	@Override
	public byte[] transform(String arg0, String arg1, byte[] arg2) {
		if (arg0.equals("blt") || arg0.contains("net.minecraft.client.renderer.EntityRenderer")) {
			String targetMethodName = "getMouseOver";
			String targetDESC = "(F)V";
			
			if(arg0.equals("blt"))
			{
				targetMethodName = patch(targetMethodName);
				targetDESC = patch(targetDESC);
			}
			
			ClassNode classNode = new ClassNode();
			ClassReader classReader = new ClassReader(arg2);
			classReader.accept(classNode, 0);
			
			Iterator<MethodNode> methods = classNode.methods.iterator();
			while(methods.hasNext())
			{
				MethodNode m = methods.next();
				if ((m.name.equals(targetMethodName) && m.desc.equals(targetDESC)))
				{
					m.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/creativemd/cmdcam/transform/CamMouseOverHandler", "setupMouseHandlerBefore", "()V", false));
					
					
					AbstractInsnNode currentNode = null;
					
					@SuppressWarnings("unchecked")
					Iterator<AbstractInsnNode> iter = m.instructions.iterator();
					
					while (iter.hasNext())
					{
						currentNode = iter.next();
						if (currentNode instanceof InsnNode && ((InsnNode)currentNode).getOpcode() == RETURN)
						{
							m.instructions.insertBefore(currentNode, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/creativemd/cmdcam/transform/CamMouseOverHandler", "setupMouseHandlerAfter", "()V", false));
						}
					}
					
					
					System.out.println("[CMDCam] Patched getMouseOver");
					break;
				}
			}
			
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			classNode.accept(writer);
			return writer.toByteArray();
			
		}
		return arg2;
	}
}
