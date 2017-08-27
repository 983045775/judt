/**
 * 
 */
package judp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import udt.UDTSession;
import udt.UDTSocket;
import udt.packets.Destination;

/**
 * @author jinyu
 *
 *����˷��ص�����ӿڶ���
 */
public class judpSocket {
private  final int bufSize=1500;
private UDTSocket socket=null;
private long start=System.currentTimeMillis();
private boolean isClose=false;
private long flushTime=0;
private final long waitDataLen=30*1000;//30��
private long  readLen=0;//��ȡ����
private long sendLen=0;//��������
public long socketID=0;//ID
private static final Logger logger=Logger.getLogger(judpSocket.class.getName());
public boolean getCloseState()
{
	return isClose;
}
public judpSocket(UDTSocket  usocket)
{
	this.socket=usocket;
	socketID=socket.getSession().getSocketID();
}

/**
 * �ر�
 */
public void close()
{
	isClose=true;
	//������ʵ�ر�
	if(sendLen==0)
	{
		//û�з��������ֱ�ӹرգ�����Ҫ�ȴ����ݷ������
		 try {
			socket.close();
			UDTSession serversession=socket.getEndpoint().removeSession(socketID);
			if(serversession!=null)
			{
				serversession.getSocket().close();
			     socket.getReceiver().stop();
			     socket.getSender().stop();
				System.out.println("����ر�socket:"+serversession.getSocketID());
			}
			
			serversession=null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		 System.out.println("����ر�socket");
	}
	else
	{
		//�й����������򻺳�
		SocketManager.getInstance().add(socket);
	}
}

/**
 * ��ȡ����
 * ���ؽ��յ��ֽڴ�С
 */
public int readData(byte[]data)
{
    if(isClose)
     {
	   return -1;
     }
	try {
	  int r=socket.getInputStream().read(data);
	  readLen+=r;
	  flushTime=System.currentTimeMillis();
	  if(flushTime-start>waitDataLen&&readLen==0)
		{
	      //�ȴ�ʱ�䳤�ȣ�û�з��͹����չ����ݣ����˳�
	       logger.info("����ʱ�䵽�˳���ȡ:"+socketID);
		   return -1;
		}
	 return r;
	} catch (IOException e) {
		e.printStackTrace();
	}
	return -1;
}

/**
 * ��ȡȫ������
 */
public byte[] readData()
{
	 byte[] result=null;
	  if(socket!=null)
	  {
		  byte[]  readBytes=new byte[bufSize];//������
		  byte[] buf=new byte[bufSize];//������
		  int index=0;
		  int r=0;
		  try {
			  while(true)
			  {
				  if(isClose)
					{
						return null;
					}
		          r=socket.getInputStream().read(readBytes);
		          if(r==-1)
		          {
		        	  break;
		          }
		          else
		          {
		              readLen+=r;
		        	  if(r==0)
		        	  {
		        		  try {
							TimeUnit.MILLISECONDS.sleep(100);
							flushTime=System.currentTimeMillis();
							if(flushTime-start>waitDataLen&&readLen==0)
							{
							    //û��ʹ�ù�����
								break;
							}
							continue;
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
		        	  }
		        	  if(r<=bufSize)
		        	  {
		        		  //result=new byte[r];
		        		  //System.arraycopy(readBytes, 0, result, 0, r);
		        		  if(index+r<buf.length)
		        		  {
		        			  System.arraycopy(readBytes, 0, buf, index, r);
		        			  index+=r;
		        		  }
		        		  else
		        		  {
		        			  //��չ������
		        			  int len=(int) (buf.length*0.75);
		        			  if(len<bufSize)
		        			  {
		        				  len=bufSize;
		        			  }
		        			  //��С��չbufSize��һ����r��
		        			  byte[] tmp=new byte[buf.length+len];
		        			  System.arraycopy(buf, 0, tmp, 0, index+1);//��������
		        			  System.arraycopy(readBytes, 0, tmp, index, r);//��������
		        			  buf=tmp;
		        			  index+=r;
		        		  }
		        	  }
		          }
			  }
		     
		} catch (IOException e) {
		
			e.printStackTrace();
		} 
		  result=new byte[index+1];//����
		  System.arraycopy(buf, 0, result, 0,index+1);//��������
	  }
	  
	  return result;
}

/*
 * ��ȡ��ʼ������
 */
public long getInitSeqNo()
{
	if(socket!=null)
	{
	   return	socket.getSession().getInitialSequenceNumber();
	}
	return 0;
}

/**
 * ���Ͱ���
 */
public int getDataStreamLen()
{
    return socket.getSession().getDatagramSize();
}


public Destination getDestination()
{

    if(socket!=null)
    {
       return   socket.getSession().getDestination();
    }
    Destination tmp = null;
    try {
        tmp = new Destination(InetAddress.getLocalHost(), 0);
    } catch (UnknownHostException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    return tmp;
}
/**
 * ��������
 * �����ݲ��ܷ���
 */
public boolean sendData(byte[]data) {
	if(isClose)
	{
		return false;
	}
	try {
		socket.getOutputStream().write(data);
		sendLen=+1;
		flushTime=System.currentTimeMillis();
		return true;
	} catch (IOException e) {
		e.printStackTrace();
	}
	return false;
}


}
