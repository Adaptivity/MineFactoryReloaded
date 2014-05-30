package powercrystals.minefactoryreloaded.gui.client;

import cofh.gui.GuiBase;
import cofh.gui.element.ElementButtonManaged;
import cofh.gui.element.ElementListBox;
import cofh.gui.element.listbox.IListBoxElement;
import cofh.gui.element.listbox.SliderVertical;
import cofh.util.position.BlockPosition;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import powercrystals.minefactoryreloaded.MFRRegistry;
import powercrystals.minefactoryreloaded.MineFactoryReloadedClient;
import powercrystals.minefactoryreloaded.MineFactoryReloadedCore;
import powercrystals.minefactoryreloaded.api.rednet.IRedNetLogicCircuit;
import powercrystals.minefactoryreloaded.gui.control.ButtonLogicBufferSelect;
import powercrystals.minefactoryreloaded.gui.control.ButtonLogicPinSelect;
import powercrystals.minefactoryreloaded.gui.control.ListBoxElementCircuit;
import powercrystals.minefactoryreloaded.gui.control.LogicButtonType;
import powercrystals.minefactoryreloaded.net.Packets;
import powercrystals.minefactoryreloaded.tile.rednet.TileEntityRedNetLogic;
import powercrystals.minefactoryreloaded.tile.rednet.TileEntityRedNetLogic.PinMapping;

public class GuiRedNetLogic extends GuiBase
{
	private class CircuitComparator implements Comparator<IRedNetLogicCircuit>
	{
		@Override
		public int compare(IRedNetLogicCircuit arg0, IRedNetLogicCircuit arg1)
		{
			return StatCollector.translateToLocal(arg0.getUnlocalizedName()).compareTo(StatCollector.translateToLocal(arg1.getUnlocalizedName()));
		}
	}
	
	private TileEntityRedNetLogic _logic;
	
	private int _selectedCircuit;
	
	private ElementListBox _circuitList;
	
	private SliderVertical _circuitScroll;
	
	private ButtonLogicBufferSelect[] _inputIOBufferButtons = new ButtonLogicBufferSelect[16];
	private ButtonLogicBufferSelect[] _outputIOBufferButtons = new ButtonLogicBufferSelect[16];
	
	private ButtonLogicPinSelect[] _inputIOPinButtons = new ButtonLogicPinSelect[16];
	private ButtonLogicPinSelect[] _outputIOPinButtons = new ButtonLogicPinSelect[16];
	
	private ElementButtonManaged _nextCircuit;
	private ElementButtonManaged _prevCircuit;
	
	private ElementButtonManaged _reinit;
	private ElementButtonManaged _reinitConfirm;
	
	private int _reinitCountdown;
	
	public GuiRedNetLogic(Container container, TileEntityRedNetLogic logic)
	{	
		super(container, new ResourceLocation(MineFactoryReloadedCore.guiFolder + "rednetlogic.png"));
		xSize = 384;
		ySize = 256;
		
		_logic = logic;
		
		_circuitList = new ElementListBox(this, 86, 16, 130, 234)
		{
			@Override
			protected void onSelectionChanged(int newIndex, IListBoxElement newElement)
			{
			}
			
			@Override
			protected void onElementClicked(IListBoxElement element)
			{
				Packets.sendToServer(Packets.LogicSetCircuit, _logic,
						_selectedCircuit, element.getValue().getClass().getName());
			}
			
			@Override
			protected void onScroll(int newStartIndex)
			{
				_circuitScroll.setValue(newStartIndex);
			}
		};
		
		List<IRedNetLogicCircuit> circuits = new LinkedList<IRedNetLogicCircuit>(MFRRegistry.getRedNetLogicCircuits());
		Collections.sort(circuits, new CircuitComparator());
		
		for(IRedNetLogicCircuit circuit : circuits)
		{
			_circuitList.add(new ListBoxElementCircuit(circuit));
		}
		
		addElement(_circuitList);
		
		_circuitScroll = new SliderVertical(this, 218, 16, 10, 234, _circuitList.getLastScrollPosition())
		{
			@Override
			public void onValueChanged(int value)
			{
				_circuitList.scrollTo(value);
			}
		};
		
		addElement(_circuitScroll);
		
		_prevCircuit = new ElementButtonManaged(this, 344, 16, 30, 30, "Prev")
		{
			@Override
			public void onClick()
			{
				_selectedCircuit--;
				if(_selectedCircuit < 0)
				{
					_selectedCircuit = _logic.getCircuitCount() - 1;
				}
				MineFactoryReloadedClient.prcPages.put(new BlockPosition(_logic), _selectedCircuit);
				requestCircuit();
			}
		};
		
		_nextCircuit = new ElementButtonManaged(this, 344, 76, 30, 30, "Next")
		{
			@Override
			public void onClick()
			{
				_selectedCircuit++;
				if(_selectedCircuit >= _logic.getCircuitCount())
				{
					_selectedCircuit = 0;
				}
				MineFactoryReloadedClient.prcPages.put(new BlockPosition(_logic), _selectedCircuit);
				requestCircuit();
			}
		};
		
		addElement(_prevCircuit);
		addElement(_nextCircuit);
		
		_reinit = new ElementButtonManaged(this, 316, 228, 60, 20, "Reinitialize")
		{
			@Override
			public void onClick()
			{
				_reinitCountdown = 40;
			}
		};
		
		_reinitConfirm = new ElementButtonManaged(this, 316, 228, 60, 20, "Confirm")
		{
			@Override
			public void onClick()
			{
				Packets.sendToServer(Packets.LogicReinitialize, _logic,
						Minecraft.getMinecraft().thePlayer.getEntityId());
				_reinitCountdown = 0;
			}
		};
		
		addElement(_reinit);
		addElement(_reinitConfirm);
		
		_reinitConfirm.setVisible(false);
		
		int rotation = logic.getWorldObj().getBlockMetadata(logic.xCoord, logic.yCoord, logic.zCoord);
		
		for(int i = 0; i < _inputIOPinButtons.length; i++)
		{
			_inputIOBufferButtons[i]  = new ButtonLogicBufferSelect(this,  22, 16 + i * 14, i, LogicButtonType.Input, rotation);
			_inputIOPinButtons[i]	 = new ButtonLogicPinSelect(   this,  52, 16 + i * 14, i, LogicButtonType.Input);
			
			_outputIOBufferButtons[i] = new ButtonLogicBufferSelect(this, 254, 16 + i * 14, i, LogicButtonType.Output, rotation);
			_outputIOPinButtons[i]	= new ButtonLogicPinSelect(   this, 284, 16 + i * 14, i, LogicButtonType.Output);
			
			addElement(_inputIOBufferButtons[i]);
			addElement(_outputIOBufferButtons[i]);
			addElement(_inputIOPinButtons[i]);
			addElement(_outputIOPinButtons[i]);
		}
		
		
		Integer lastPage = MineFactoryReloadedClient.prcPages.get(new BlockPosition(_logic));
		if(lastPage != null && lastPage < _logic.getCircuitCount())
		{
			_selectedCircuit = lastPage;
		}
		requestCircuit();
	}
	
	public PinMapping getInputPin(int pinIndex)
	{
		return _logic.getInputPinMapping(_selectedCircuit, pinIndex);
	}
	
	public PinMapping getOutputPin(int pinIndex)
	{
		return _logic.getOutputPinMapping(_selectedCircuit, pinIndex);
	}
	
	private void requestCircuit()
	{
		Packets.sendToServer(Packets.LogicRequestCircuitDefinition, _logic,
				_selectedCircuit);
	}
	
	@Override
	public void updateScreen()
	{
		if(((IRedNetLogicCircuit)_circuitList.getSelectedElement().getValue()).getClass() != _logic.getCircuit(_selectedCircuit).getClass())
		{
			for(int i = 0; i < _circuitList.getElementCount(); i++)
			{
				if(((IRedNetLogicCircuit)_circuitList.getElement(i).getValue()).getClass() == _logic.getCircuit(_selectedCircuit).getClass())
				{
					_circuitList.setSelectedIndex(i);
					_circuitScroll.setValue(Math.min(i, _circuitList.getLastScrollPosition()));
					break;
				}
			}
		}
		
		for(int i = 0; i < _inputIOPinButtons.length; i++)
		{
			if(i < _logic.getCircuit(_selectedCircuit).getInputCount())
			{
				_inputIOPinButtons[i].setVisible(true);
				_inputIOBufferButtons[i].setVisible(true);
				_inputIOPinButtons[i].setPin(_logic.getInputPinMapping(_selectedCircuit, i).pin);
				_inputIOPinButtons[i].setBuffer(_logic.getInputPinMapping(_selectedCircuit, i).buffer);
				_inputIOBufferButtons[i].setBuffer(_logic.getInputPinMapping(_selectedCircuit, i).buffer);
			}
			else
			{
				_inputIOBufferButtons[i].setVisible(false);
				_inputIOPinButtons[i].setVisible(false);
			}
		}
		
		for(int i = 0; i < _outputIOPinButtons.length; i++)
		{
			if(i < _logic.getCircuit(_selectedCircuit).getOutputCount())
			{
				_outputIOBufferButtons[i].setVisible(true);
				_outputIOPinButtons[i].setVisible(true);
				_outputIOPinButtons[i].setPin(_logic.getOutputPinMapping(_selectedCircuit, i).pin);
				_outputIOPinButtons[i].setBuffer(_logic.getOutputPinMapping(_selectedCircuit, i).buffer);
				_outputIOBufferButtons[i].setBuffer(_logic.getOutputPinMapping(_selectedCircuit, i).buffer);
			}
			else
			{
				_outputIOBufferButtons[i].setVisible(false);
				_outputIOPinButtons[i].setVisible(false);
			}
		}
		
		if(_reinitCountdown > 0)
		{
			_reinitCountdown--;
		}
		
		_reinit.setVisible(_reinitCountdown == 0);
		_reinitConfirm.setVisible(_reinitCountdown > 0);
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
	{
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
		
		fontRendererObj.drawString("Programmable RedNet Controller", 8, 6, 4210752);
		fontRendererObj.drawString((_selectedCircuit + 1) + " of " + _logic.getCircuitCount(), 336, 60, 4210752);
		
		for(int i = 0; i < _inputIOPinButtons.length; i++)
		{
			if(i < _logic.getCircuit(_selectedCircuit).getInputCount())
			{
				fontRendererObj.drawString(_logic.getCircuit(_selectedCircuit).getInputPinLabel(i), 4, 20 + i * 14, 4210752);
			}
		}
		
		for(int i = 0; i < _outputIOPinButtons.length; i++)
		{
			if(i < _logic.getCircuit(_selectedCircuit).getOutputCount())
			{
				fontRendererObj.drawString(_logic.getCircuit(_selectedCircuit).getOutputPinLabel(i), 232, 20 + i * 14, 4210752);
			}
		}
	}
	
	public void setInputPinMapping(int index, int buffer, int pin)
	{
		Packets.sendToServer(Packets.LogicSetPin, _logic,
				 (byte)0, _selectedCircuit, index, buffer, pin);
	}
	
	public void setOutputPinMapping(int index, int buffer, int pin)
	{
		Packets.sendToServer(Packets.LogicSetPin, _logic,
				 (byte)1, _selectedCircuit, index, buffer, pin);
	}
	
	public int getVariableCount()
	{
		return _logic.getVariableBufferSize();
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
	{
		mouseX -= guiLeft;
		mouseY -= guiTop;
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		bindTexture(texture);
		drawLargeTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
		
		GL11.glPushMatrix();
		GL11.glTranslatef(guiLeft, guiTop, 0.0F);
		drawElements(gameTicks, false);
		GL11.glPopMatrix();
	}
	
	public void drawLargeTexturedModalRect(int x, int y, int u, int v, int xSize, int ySize)
	{
		float uScale = 1.0F/384.0F;
		float vScale = 1.0F/256.0F;
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.addVertexWithUV(x + 0, y + ySize, this.zLevel, (u + 0) * uScale, (v + ySize) * vScale);
		tessellator.addVertexWithUV(x + xSize, y + ySize, this.zLevel, (u + xSize) * uScale, (v + ySize) * vScale);
		tessellator.addVertexWithUV(x + xSize, y + 0, this.zLevel, (u + xSize) * uScale, (v + 0) * vScale);
		tessellator.addVertexWithUV(x + 0, y + 0, this.zLevel, (u + 0) * uScale, (v + 0) * vScale);
		tessellator.draw();
	}
}
