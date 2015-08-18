import java.util.ArrayList;


public class ControlFlow {

	Node start;
	Node temp;
	ArrayList<Integer> nodes_created=new ArrayList<Integer>();
	public Node addBlock(Node node)
	{
		
		if(start==null)
		{
			start=node;	
			nodes_created.add(node.block_number);
		}		
		else
		{				
			for(int i=0;i<node.successors.size();i++)
			{
			  if(node.successors.get(i).block_number==node.block_number)
			  {
				  
			  }
			}
			if(!nodes_created.contains(node.block_number))
			{
				start.successors.add(node);
				nodes_created.add(node.block_number);
				for(int i=0;i<start.successors.size();i++)
				{
					temp=start.successors.get(i);
					node.predecessor.add(temp);				
				}   
			}
			else
			{
				System.out.println(node.block_number+" has already been created");
			}
		/*	
			for(int i=0;i<node.predecessor.size();i++)
			{
		//	System.out.println("node value of "+node.block_number+" "+ start.successors.get(i).block_number);
			}  */
			start=node;			
			
		}
		return node;
	}
	
	
	public Node findBlock(int block_num)
	{
		if(start==null)
			return null;
		if(start.block_number==block_num)
		{
			return start;
		}
		
		Node node=start;
		
		for(int i=0;i<node.successors.size();i++)
		{
			node=node.successors.get(i);
			if(node.block_number==block_num)
				return node;
		}   
		
		return null;
	}
	
	
}
