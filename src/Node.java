import java.util.ArrayList;


class Node
{
	    ArrayList<Node> predecessor;
	    ArrayList<Node>	successors;
		int block_number;
		
		Node(int block_number)
		{
			this.block_number=block_number;
			this.predecessor = new ArrayList<>();
			this.successors=new ArrayList<Node>();
		}
		
			
}
	
	
	
