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
				
				method.instructions.add(asmapi.buildMethodCall("team/creative/cmdcam/client/CamEventHandlerClient", "shouldPlayerTakeInput", "()Z", asmapi.MethodType.STATIC));
				method.instructions.add(new InsnNode(Opcodes.IRETURN));

                return method;
            }
		},
		'renderPlayer': {
            'target': {
                'type': 'METHOD',
				'class': 'net.minecraft.client.renderer.entity.PlayerRenderer',
				'methodName': 'func_76986_a',
				'methodDesc': '(Lnet/minecraft/client/entity/player/AbstractClientPlayerEntity;DDDFF)V'
            },
            'transformer': function(method) {
				var Opcodes = Java.type('org.objectweb.asm.Opcodes');
				var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI');
				var before = asmapi.findFirstInstruction(method, Opcodes.IF_ACMPNE);
				for(var i = 0; i < 6; i++){
					before = before.getPrevious();
					method.instructions.remove(before.getNext());
				}

                return method;
            }
		}
    }
}
