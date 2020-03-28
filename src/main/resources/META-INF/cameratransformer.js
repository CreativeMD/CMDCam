function initializeCoreMod() {
	print("Init CMDCam coremods ...")
    return {
        'changeMouseOver': {
            'target': {
                'type': 'METHOD',
				'class': 'net.minecraft.client.renderer.GameRenderer',
				'methodName': 'func_78473_a',
				'methodDesc': '(F)V'
            },
            'transformer': function(method) {
				var Opcodes = Java.type('org.objectweb.asm.Opcodes');
				var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI');
				
				method.instructions.insert(asmapi.buildMethodCall("team/creative/cmdcam/client/CamEventHandlerClient", "setupMouseHandlerBefore", "()V", asmapi.MethodType.STATIC));
				
				var node = asmapi.findFirstInstruction(method, Opcodes.RETURN);
				method.instructions.insertBefore(node, asmapi.buildMethodCall("team/creative/cmdcam/client/CamEventHandlerClient", "setupMouseHandlerAfter", "()V", asmapi.MethodType.STATIC));
                return method;
            }
		},
		'currentEntity': {
            'target': {
                'type': 'METHOD',
				'class': 'net.minecraft.client.entity.player.ClientPlayerEntity',
				'methodName': 'func_175160_A',
				'methodDesc': '()Z'
            },
            'transformer': function(method) {
				var Opcodes = Java.type('org.objectweb.asm.Opcodes');
				var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI');
				var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
				
				method.instructions.clear();
				
				method.instructions.add(asmapi.buildMethodCall("team/creative/cmdcam/client/CamEventHandlerClient", "isCurrentViewEntity", "()Z", asmapi.MethodType.STATIC));
				method.instructions.add(new InsnNode(Opcodes.IRETURN));

                return method;
            }
		},
		'renderPlayer': {
            'target': {
                'type': 'METHOD',
				'class': 'net.minecraft.client.renderer.WorldRenderer',
				'methodName': 'func_228426_a_',
				'methodDesc': '(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/renderer/Matrix4f;)V'
            },
            'transformer': function(method) {
				var Opcodes = Java.type('org.objectweb.asm.Opcodes');
				var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI');
				var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
				
				var node = null;
				var index = 0;
				
				while(node == null || node.desc !== 'net/minecraft/client/entity/player/ClientPlayerEntity') {
					node = asmapi.findFirstInstructionAfter(method, Opcodes.INSTANCEOF, index);
					index = method.instructions.indexOf(node) + 1;	
				}
				
				var jump = node.getNext();
				var before = node.getPrevious();
				
				method.instructions.insertBefore(before, asmapi.buildMethodCall("team/creative/cmdcam/client/CamEventHandlerClient", "isPathActive", "()Z", asmapi.MethodType.STATIC));
				method.instructions.insertBefore(before, new JumpInsnNode(Opcodes.IFNE, jump.label));
				
                return method;
            }
		}
    }
}
