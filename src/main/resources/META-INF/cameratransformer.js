function initializeCoreMod() {
	print("Init CMDCam coremods ...")
    return {
        'changeMouseOver': {
            'target': {
                'type': 'METHOD',
				'class': 'net.minecraft.client.renderer.GameRenderer',
				'methodName': 'm_109087_',
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
				'class': 'net.minecraft.client.player.LocalPlayer',
				'methodName': 'm_108636_',
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
				'class': 'net.minecraft.client.renderer.LevelRenderer',
				'methodName': 'm_109599_',
				'methodDesc': '(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lcom/mojang/math/Matrix4f;)V'
			},
            'transformer': function(method) {
				var Opcodes = Java.type('org.objectweb.asm.Opcodes');
				var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI');
				var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
				
				var node = null;
				var index = 0;
				
				while(node == null || node.desc !== 'net/minecraft/client/player/LocalPlayer') {
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
