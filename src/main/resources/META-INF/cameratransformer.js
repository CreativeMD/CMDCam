function initializeCoreMod() {
	print("Init CMDCam coremods ...")
    return {
        'changeMouseOver': {
            'target': {
                'type': 'METHOD',
				'class': 'net.minecraft.client.renderer.GameRenderer',
				'methodName': 'getMouseOver',
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
				'methodName': 'isCurrentViewEntity',
				'methodDesc': '()Z'
            },
            'transformer': function(method) {
				var Opcodes = Java.type('org.objectweb.asm.Opcodes');
				var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI');
				var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
				
				method.instructions.clear();
				
				method.instructions.add(asmapi.buildMethodCall("team/creative/cmdcam/client/CamEventHandlerClient", "shouldPlayerTakeInput", "()Z", asmapi.MethodType.STATIC));
				method.instructions.add(new InsnNode(Opcodes.IRETURN));

                return method;
            }
		},
		'renderPlayer': {
            'target': {
                'type': 'METHOD',
				'class': 'net.minecraft.client.renderer.entity.PlayerRenderer',
				'methodName': 'doRender',
				'methodDesc': '(Lnet/minecraft/client/entity/player/AbstractClientPlayerEntity;DDDFF)V'
            },
            'transformer': function(method) {
				var Opcodes = Java.type('org.objectweb.asm.Opcodes');
				var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI');
				var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
				var before = asmapi.findFirstInstruction(method, Opcodes.IF_ACMPNE);
				for(var i = 0; i < 6; i++){
					before = before.getPrevious();
					method.instructions.remove(before.getNext());
				}
				
				//method.instructions.set(before, new JumpInsnNode(Opcodes.IF_ACMPEQ, before.label));

                return method;
            }
		}
    }
}
