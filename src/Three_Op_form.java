import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Iterables;


public class Three_Op_form {
	private static ArrayList<HashMap<Integer, String>> struc;
	private static ArrayList<HashMap<Integer, String>> initial_block;
	

	public static void main(String args[]) throws IOException {
		// String path=System.getProperty("user.dir");
		// File three_op_form=Convert_to_Three_OP_Form(args[0]);
		File dependency_file = Prepare_dependency_blocks(args[0]);
		HashMap<Integer, ArrayList<Integer>> successorsList = Construct_Blocks(dependency_file.getName());
		
		
		
		HashMap<Integer, ArrayList<Integer>> gen_table = Generate_Table(struc);
		HashMap<Integer, ArrayList<Integer>> kill_table = Kill_Table(struc);
		
		HashMap<String,HashMap<Integer, ArrayList<Integer>>> gen_kill_in_out=Gen_Kill_UNION(gen_table,kill_table,successorsList);
		System.out.println("GEN:KILL:IN:OUT\n"+gen_kill_in_out);
		System.out.println("Struc : \n"+struc);
		
		struc=Constant_Propagation(struc);
		System.out.println("After Constant Propagation: \n"+struc);
//		struc=Constant_folding(struc);
//		System.out.println("After Constant Folding: \n"+struc);
		

	}



	private static File Convert_to_Three_OP_Form(String fileName)
			throws IOException {

		File f = new File(fileName);
		PrintWriter pw = new PrintWriter("3opform.txt");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		int variable_count = 0;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (line.contains("[") && line.contains("]")) {
				Pattern pattern = Pattern.compile("(\\w+)(\\[)(.*?)(\\])");
				Matcher matcher = pattern.matcher(line);
				List<String> lineMatches = new ArrayList<String>();
				while (matcher.find()) {
					lineMatches.add(matcher.group(0));
					if (line.contains("if") && line.contains("goto")) {
						String condt = matcher.group(0);
						String variable = matcher.group(3);
						if (variable.matches("\\d+")) {
							pw.println(line);
						} else {
							pw.println("let t_" + variable_count++ + "= "
									+ variable);
							pw.println("let t_"
									+ variable_count++
									+ "= "
									+ condt.replace(variable, "t_"
											+ (variable_count - 1)));
							pw.println(line.replace(condt, "t_"
									+ (variable_count)));
						}
					}

				}

				Collections.reverse(lineMatches);
				for (int i = 0; i < lineMatches.size(); i++) {
					String word_list = lineMatches.get(i);
					String factor1 = word_list.substring(
							word_list.indexOf("[") + 1, word_list.indexOf("]"));
					if (factor1.matches("\\d+")) {
						pw.println(line);
					} else {
						pw.println("let t_" + variable_count++ + "= " + factor1);
						word_list = word_list.replace(factor1, "t_"
								+ (variable_count - 1));
						pw.println("let t_" + variable_count++ + "= "
								+ word_list);

					}

				}

			}

			else {
				pw.println(line);
			}
		}
		pw.close();
		br.close();
		return f;

	}

	public static File Prepare_dependency_blocks(String filename)
			throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));
		File f = new File("1.txt");
		PrintWriter pw = new PrintWriter(f);
		String line;

		while ((line = br.readLine()) != null) {
			if (line.contains("begin")) {
				pw.print("\n");
				pw.println(line);
			}

			else if (line.contains("label")) {
				pw.print("\n");
				pw.println(line);
			} else if (line.contains("goto")) {
				pw.println(line);
				pw.print("\n");

			} else {
				pw.println(line);
			}

		}

		pw.close();
		br.close();

		return f;

	}

	private static HashMap<Integer, ArrayList<Integer>> Construct_Blocks(
			String file) throws IOException {

		File f = new File(file);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		struc = new ArrayList<HashMap<Integer, String>>();
		LinkedHashMap<Integer, String> block = new LinkedHashMap<Integer, String>();
		int line_num = 1;
		// ///////////// most imp part
		while ((line = br.readLine()) != null) {
			if (line.trim().length() == 0) {
				struc.add(block);
				block = new LinkedHashMap<Integer, String>();
				continue;
			}
			block.put(line_num++, line);
		}
		struc.add(block);

		// remove empty elements

		HashMap<String, Integer> goto_table = new HashMap<String, Integer>();
		HashMap<String, Integer> label_table = new HashMap<String, Integer>();

		Iterator<HashMap<Integer, String>> itr = struc.iterator();

		// remove all empty elements...use iterator to avoid concurrent
		// modification exception
		while (itr.hasNext()) {
			if (itr.next().isEmpty()) {
				itr.remove();
			}
		}

		for (int i = 0; i < struc.size(); i++) {

			HashMap<Integer, String> hmap = struc.get(i);
			for (Entry<Integer, String> entry : hmap.entrySet()) {
				if (entry.getValue().contains("goto")) {
					String label = entry.getValue().substring(
							entry.getValue().indexOf("goto") + 5,
							entry.getValue().length());
					goto_table.put(label.trim(), i - 1);
				} else if (entry.getValue().contains("label")) {
					String label = entry.getValue().substring(
							entry.getValue().indexOf("label") + 5,
							entry.getValue().length());
					label_table.put(label.trim(), i - 1);
				}
			}

		}


		HashMap<Integer, ArrayList<Integer>> successorsList = Construct_ControlFlow(struc, goto_table, label_table);
		return successorsList;
	}

	private static HashMap<Integer, ArrayList<Integer>> Construct_ControlFlow(
			ArrayList<HashMap<Integer, String>> struc,
			HashMap<String, Integer> goto_table,
			HashMap<String, Integer> label_table) throws FileNotFoundException {

		ControlFlow cf = new ControlFlow();

		String last_line;
		int count_line = 0;
		int block_redirection = 0;
		int current_block = -1;
		int predecessor = -1;
		HashMap<Integer, String> initial_block = struc.remove(0);
		ArrayList<String> blocks_not_visited = new ArrayList<String>();
		LinkedHashMap<Integer, ArrayList<Integer>> hmap_successors_list = new LinkedHashMap<Integer, ArrayList<Integer>>();
				
		for (int i = 0; i < struc.size(); i++) {
			blocks_not_visited.add("BLOCK_" + i);
		}

		for (int i = 0; i <= struc.size() - 1; i++) {

			HashMap<Integer, String> hmap = struc.get(block_redirection);
			count_line = 0;
			predecessor = current_block;
			current_block = block_redirection;
			if (!blocks_not_visited.isEmpty()&& blocks_not_visited.contains("BLOCK_" + block_redirection)) {
				for (Entry<Integer, String> entry : hmap.entrySet()) {
					// creating control flow structures
					//current_block = block_redirection;

					count_line++;
					if (hmap.entrySet().size() == count_line) {
						last_line = ((Entry<Integer, String>) entry).getValue();
						// System.out.println(last_line);
						if (last_line.contains("goto")&& last_line.contains("if")) {
							String label = entry.getValue().substring(entry.getValue().indexOf("goto") + 5,	entry.getValue().length()).trim();
							int block = label_table.get(label);

							if (block == goto_table.get(label)&& blocks_not_visited.contains("BLOCK_"+ block)) {
								// Node block_new = new Node(current_block);
								// block_new.successors.add(new Node(block));
								// block_new.successors.add(new
								// Node(current_block+1));
								// cf.addBlock(block_new);
								if (blocks_not_visited.isEmpty()) {
									continue;
								}
								String next_block = blocks_not_visited.get(0);
								String block_num = next_block.substring(
										next_block.indexOf("_") + 1,
										next_block.length()).trim();
								block_redirection = Integer.parseInt(block_num);
								blocks_not_visited.remove("BLOCK_" + block_num);
								i = block - 1;
								ArrayList<Integer> al = new ArrayList<Integer>();
								al.add(current_block + 1);
								al.add(block_redirection);
								
								hmap_successors_list.put(current_block, al);

								
							}

							else {
								// Node block_new = new Node(current_block);
								// block_new.successors.add(new Node(block));
								// block_new.successors.add(new
								// Node(current_block+1));
								// cf.addBlock(block_new);
								blocks_not_visited.remove("BLOCK_"+ current_block);
								block_redirection = block;
								i = block - 1;
								ArrayList<Integer> al = new ArrayList<Integer>();
								al.add(current_block + 1);
								al.add(block_redirection);
								
								hmap_successors_list.put(current_block, al);

								
							}
						}

						else if (last_line.contains("goto")
								&& !last_line.contains("if")) {
							String label = entry.getValue().substring(entry.getValue().indexOf("goto") + 5,
											entry.getValue().length()).trim();
							int block = label_table.get(label);

							if (block == goto_table.get(label)&& blocks_not_visited.contains("BLOCK_"+ block)) {
								// Node block_new = new Node(current_block);
								// block_new.successors.add(new
								// Node(current_block+1));
								// cf.addBlock(block_new);
								blocks_not_visited.remove("BLOCK_"+ current_block);
								block_redirection = block;
								i = block - 1;
								ArrayList<Integer> al = new ArrayList<Integer>();
								al.add(block_redirection);
								hmap_successors_list.put(current_block, al);
								
								
							} else if (blocks_not_visited.contains("BLOCK_"+ block)) {
								// Node block_new = new Node(current_block);
								// block_new.successors.add(new
								// Node(current_block+1));
								// cf.addBlock(block_new);
								blocks_not_visited.remove("BLOCK_"
										+ current_block);
								block_redirection = block;
								i = block - 1;
								ArrayList<Integer> al = new ArrayList<Integer>();
								al.add(block_redirection);
								hmap_successors_list.put(current_block, al);

								
							}

						} else if (blocks_not_visited.contains("BLOCK_"+ current_block)) {

							if (!blocks_not_visited.isEmpty()) {
								String next_block = blocks_not_visited.get(0);
								// Node block_new = new Node(current_block);
								// block_new.successors.add(new
								// Node(current_block+1));
								// cf.addBlock(block_new);
								blocks_not_visited.remove("BLOCK_"+ current_block);
								String block_num = next_block.substring(
										next_block.indexOf("_") + 1,
										next_block.length()).trim();
								block_redirection = Integer.parseInt(block_num);
								i = block_redirection - 1;
								ArrayList<Integer> al = new ArrayList<Integer>();
								al.add(current_block + 1);
								hmap_successors_list.put(current_block, al);

								
							}

							else {
								// Node block_new = new Node(current_block);
								// block_new.successors.add(new Node(-999));
								// cf.addBlock(block_new);
								blocks_not_visited.remove("BLOCK_"+ current_block);
								ArrayList<Integer> al = new ArrayList<Integer>();
								al.add(current_block + 1);
								hmap_successors_list.put(current_block, al);

								
							}

						}

					}
				}
			}

			else {
				block_redirection = block_redirection + 1;
			}

		}

		// System.out.println(blocks_not_visited);

		//System.out.println(hmap_successors_list);
		//System.out.println(hmap_predecessor_list);
		
		for(int i=0;i<hmap_successors_list.size();i++)
		{
			if(hmap_successors_list.get(i)!=null&& i!=hmap_successors_list.size()-1)
			{
			ArrayList<Integer> al=hmap_successors_list.get(i);
			hmap_successors_list.remove(hmap_successors_list.get(i));
			Collections.sort(al);
			hmap_successors_list.put(i, al);
			}
			else
			{
				ArrayList<Integer> al=new ArrayList<Integer>();
				al.add(-999);
				hmap_successors_list.put(i, al);
			}
		}
		
		
		
	    Phase_Two_Text( hmap_successors_list,initial_block, struc,goto_table);
        // Convert successors list into topological order.
		
		
		
		return hmap_successors_list;
	}	

	
	private static void Phase_Two_Text(
			HashMap<Integer, ArrayList<Integer>> hmap_successors_list,
			HashMap<Integer, String> initial_block,
			ArrayList<HashMap<Integer, String>> struc,HashMap<String, Integer> goto_table)
			throws FileNotFoundException {

		int count_line = 0;
		HashMap<Integer, ArrayList<Integer>> hmap_predecessor_list = new HashMap<Integer, ArrayList<Integer>>();
		PrintWriter pw = new PrintWriter("Phase_Two.txt");
		for (Entry<Integer, String> entry : initial_block.entrySet()) {
			pw.println(entry.getValue());
		}
		
		// prepare predecessor list from successors list
		
		for(Entry<Integer, ArrayList<Integer>> pred:hmap_successors_list.entrySet())
		{
			Integer key=pred.getKey();
		    ArrayList<Integer> al_pred=new ArrayList<Integer>();
			for(int i=0;i<hmap_successors_list.size();i++)
			{
			  if(hmap_successors_list.get(i)!=null)
			  {
			   ArrayList<Integer> hmap = hmap_successors_list.get(i);
			   			  
			   for(int j=0;j<hmap.size();j++)
			   {
				   if(key==hmap.get(j))
				   {
					   if(key!=i)
					   {
					   al_pred.add(i);
					   hmap_predecessor_list.put(key, al_pred);
					   }
				   }
			   }
			  }
			}
		}
	//	System.out.println("Updated pred list: "+hmap_predecessor_list);
		StringBuilder succ_list = new StringBuilder();
		StringBuilder pred_list = new StringBuilder();
		for (int i = 0; i < struc.size() ; i++) {
			HashMap<Integer, String> hmap = struc.get(i);
			count_line = 0;
			succ_list.delete(0, succ_list.length());
			pred_list.delete(0, pred_list.length());
			for (Entry<Integer, String> entry : hmap.entrySet()) {
				count_line++;

				if (count_line == 1 && i == 0)
				{
					pw.println(entry.getValue());					
						for (int ii = 0; ii < hmap_successors_list.get(i).size(); ii++) 
						{
							succ_list = succ_list.append((hmap_successors_list.get(i).get(ii)).toString() + " ");

						}
						pw.println("rem * block " + i + " pred entry"
								+ " succ " + succ_list);
				}				

				else if (count_line == 1 && i == struc.size() - 1) {
					
					  for (int ii = 0; ii < hmap_successors_list.get(i).size() ; ii++) 
						{
							pred_list = pred_list.append((hmap_predecessor_list.get(i).get(ii)).toString() + " ");

						}
						pw.println("rem * block " + i + " pred " + pred_list
								+ " succ exit");				

					pw.println(entry.getValue());
				} 
				
				else if (count_line == 1) {
					
						for (int ii = 0; ii < hmap_successors_list.get(i)
								.size(); ii++) {
							succ_list.append(hmap_successors_list.get(i)
									.get(ii) + " ");
						}
						for (int ii = 0; ii < hmap_predecessor_list.get(i)
								.size(); ii++) {
							pred_list.append(hmap_predecessor_list.get(i).get(
									ii)
									+ " ");
						}
						pw.println("rem * block " + i + " pred " + pred_list
								+ " succ " + succ_list);
					

					pw.println(entry.getValue());
				} 
				
				else {
					pw.println(entry.getValue());

				}
			}
		}
		pw.close();	
        System.out.println("Succcessors list "+hmap_successors_list);
        System.out.println("Predecessors list "+hmap_predecessor_list);
        	
		
	}

	public static HashMap<Integer, ArrayList<Integer>> Generate_Table(
			ArrayList<HashMap<Integer, String>> struc) {
		HashMap<Integer, ArrayList<Integer>> generate_tab = new HashMap<Integer, ArrayList<Integer>>();
		for (int i = 0; i < struc.size(); i++) {
			HashMap<Integer, String> hmap = struc.get(i);
			ArrayList<Integer> list_gen = new ArrayList<Integer>();
			for (Entry<Integer, String> entry : hmap.entrySet()) {

				String line = entry.getValue();
				if (line.trim().startsWith("let")) {
					list_gen.add(entry.getKey());
				}

			}
			generate_tab.put(i, list_gen);
		}
		System.out.println("Generate Table values: \n"+generate_tab);
		return generate_tab;

	}

	private static HashMap<Integer, ArrayList<Integer>> Kill_Table(	ArrayList<HashMap<Integer, String>> struc) {

		HashMap<Integer, ArrayList<Integer>> kill_tab = new HashMap<Integer, ArrayList<Integer>>();
		HashMap<Integer, HashMap<String, String>> redundant_def=new HashMap<Integer, HashMap<String, String>>();
		HashMap<String, String> block_def=null;
		// multimap to check kill definition
		//Multimap<String, String> multiMap = ArrayListMultimap.create();
		for (int i = 0; i < struc.size(); i++)
		{
			HashMap<Integer, String> hmap = struc.get(i);
			//redundant_def = new HashMap<Integer, HashMap<String, String>>();
			block_def = new HashMap<String, String>();
			for (Entry<Integer, String> entry : hmap.entrySet()) 
			{
				String line = entry.getValue();
				if (line.trim().startsWith("let"))
				{					
					String factor1 = line.substring(line.indexOf("let") + 3,
							line.indexOf("=")).trim();
					block_def.put(factor1, entry.getKey().toString());	
					String factor2 = line.substring(line.indexOf("="),line.length()).trim();
					if(factor2.contains("phi"))
					{
						String factor3=factor2.substring(factor2.indexOf("(")+1, factor2.indexOf(")")+1).trim();
						String[] indices=factor3.split(",");
						for(int j=0;j<indices.length;j++)
						{
							block_def.put(indices[j], entry.getKey().toString());
						}
						
					}

				}	
				
			}
			redundant_def.put(i, block_def);			
		}
		System.out.println("SYMBOL TABLE DEF:\n"+redundant_def);
		
		ArrayList<Integer> al=null;
		ArrayList<String> same_kill_def=null;
				
		for (int i = 0; i < struc.size(); i++) {
			HashMap<Integer, String> hmap = struc.get(i);
			//redundant_def = new HashMap<Integer, HashMap<String, String>>();		
			al=new ArrayList<Integer>();
			for (Entry<Integer, String> entry : hmap.entrySet()) 
			{
				String line = entry.getValue();
				if (line.trim().startsWith("let")) 
				{
					String factor1 = line.substring(line.indexOf("let") + 3,
							line.indexOf("=")).trim();
					
					
					for (int ii=0;ii<redundant_def.size();ii++) {
					HashMap<String, String> hmap_red= redundant_def.get(ii);
					
					if(!hmap_red.equals(null))
					{
						for (Entry<String, String> entry_1 : hmap_red.entrySet())
						{ 
							
							if(factor1.equals(entry_1.getKey()))
							{	
								if(i==ii)
								{
									al.remove(entry.getValue());
								}
								else
								{								
								Integer kill_line=Integer.parseInt(entry_1.getValue());					    	 	
								al.add(kill_line);
								}
								
							}
						}
					}
					
					}
					
					kill_tab.put(i, al);
					
				}
			}
			
		}
		
		HashMap<Integer, ArrayList<Integer>> kill_tab_expand = new HashMap<Integer, ArrayList<Integer>>();
		for(int i=0;i<struc.size();i++)
		{			
			kill_tab_expand.put(i, new ArrayList<Integer>());
		}
		
		for(Entry<Integer, ArrayList<Integer>> exp:kill_tab.entrySet())
		{
		
			kill_tab_expand.put(exp.getKey(), exp.getValue());
		}
		
		System.out.println("KILL TABLE DEF:\n"+kill_tab_expand);
		return kill_tab_expand;
	}
	
	
	private static HashMap<String,HashMap<Integer, ArrayList<Integer>>> Gen_Kill_UNION(
			HashMap<Integer, ArrayList<Integer>> gen_table,
			HashMap<Integer, ArrayList<Integer>> kill_table, HashMap<Integer, ArrayList<Integer>> successorsList) {
		
		HashMap<Integer, ArrayList<Integer>> in_table=new HashMap<Integer, ArrayList<Integer>>();
		HashMap<Integer, ArrayList<Integer>> out_table=new HashMap<Integer, ArrayList<Integer>>();
		
		HashMap<String,HashMap<Integer, ArrayList<Integer>>> gen_kill_in_out=new HashMap<String,HashMap<Integer, ArrayList<Integer>>>();
		
		//Prepare predecessors list from successors list
		
		
		HashMap<Integer, ArrayList<Integer>> hmap_predecessor_list = new HashMap<Integer, ArrayList<Integer>>();
		
		// prepare predecessor list from successors list
		
		for(Entry<Integer, ArrayList<Integer>> pred:successorsList.entrySet())
		{
			Integer key=pred.getKey();
		    ArrayList<Integer> al_pred=new ArrayList<Integer>();
			for(int i=0;i<successorsList.size();i++)
			{
			   ArrayList<Integer> hmap = successorsList.get(i);
			   			  
			   for(int j=0;j<hmap.size();j++)
			   {
				   if(key==hmap.get(j))
				   {
					   al_pred.add(i);
					   hmap_predecessor_list.put(key, al_pred);

				   }
			   }
			}
		}
		
		// out=gen U (in-kill)
		// in= output out of previous block

		// Convert Successor list to Single set
		int next_level=0;
		LinkedHashSet<Integer> control_block_FLow=new LinkedHashSet<Integer>();
		control_block_FLow.add(0);
		for(int i=0;i<successorsList.size();i++)
		{
			 ArrayList<Integer> next=successorsList.get(next_level);
		     control_block_FLow.addAll(next);
		     
		     if(next_level==next.get(0))
		     {
		    	 next_level=next.get(1);
		     }
		     else
		     {
		     next_level=next.get(0);
		     }
		}
				
		ArrayList<Integer> control_block_FLow_COPY=new ArrayList<Integer>(control_block_FLow);
		
		LinkedHashSet<Integer> additions_successors_from_old=new LinkedHashSet<Integer>(control_block_FLow_COPY);

		System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("Initial worklist :"+control_block_FLow);
		
	//	additions_successors_from_old= control_block_FLow;
		
       
		
		for(int i=0;i<struc.size();i++)
		{
			in_table.put(i, new ArrayList<Integer>());
			out_table.put(i, new ArrayList<Integer>());
		}
				
		// fill all the in_table and out_table to null
		
			ArrayList<Integer> al_old = null;
			ArrayList<Integer> al_gen=null;
			ArrayList<Integer> al_kill=null;
		
			
            Integer next_block=0, present_block=0,previous_block=-1;
            
         //   ListIterator<Integer> itr = control_block_FLow_COPY.listIterator();
            
            Iterator<Integer> itr = Iterables.cycle(control_block_FLow_COPY).iterator();
            
		do {
			if (itr.hasNext()) {
				next_block = itr.next();

				previous_block = present_block;
				present_block = next_block;

				if (gen_table.get(present_block) != null) {
					al_gen = gen_table.get(present_block);
				} else {
					gen_table.put(present_block, new ArrayList<Integer>());
				}

				if (kill_table.get(present_block) != null) {
					al_kill = kill_table.get(present_block);
				} else {
					gen_table.put(present_block, new ArrayList<Integer>());
				}

				// Step :1

				itr.remove();
				additions_successors_from_old.remove(present_block);
				

				// Step:2
				al_old = new ArrayList<Integer>();
				al_old = out_table.get(present_block);

				// Step:Process in and out

				// in_table.get(i); default is empty for first time
				// in=gen(n) U (in(n)-kill(n))

				// ////////// in union of out for predecessors of that block    && present_block!=struc.size()-1
				if (present_block != 0) {
					ArrayList<Integer> pred_of_block = hmap_predecessor_list.get(present_block);
					LinkedHashSet<Integer> out_tab_union = new LinkedHashSet<Integer>();
					for (int i = 0; i < pred_of_block.size(); i++) {
						out_tab_union.addAll(out_table.get(pred_of_block.get(i)));
					}
					in_table.get(present_block).addAll(out_tab_union);
				} else {
					in_table.put(present_block, new ArrayList<Integer>());
				}

				// ///////////////out==   (GEN U IN )-KILL
							
				ArrayList<Integer> c1_gen_in = new ArrayList<Integer>();
				c1_gen_in.addAll(al_gen);
				c1_gen_in.addAll(in_table.get(present_block));
				
				Collection<Integer> c2_union_kill=  new ArrayList<Integer>();
				c2_union_kill.addAll(al_kill);
				
		        c1_gen_in.removeAll(c2_union_kill);

				out_table.put(present_block, c1_gen_in);

				
				in_table=RemoveDuplicates_Intable(in_table);
				out_table=RemoveDuplicates_Outtable(out_table);
				
                
				// remove duplicates in in _tables   out_table in values pair
				
			
				
				
				if (!al_old.equals(out_table.get(present_block)) && present_block!=struc.size()-1) {
					ArrayList<Integer> remainder=successorsList.get(present_block);
					//  removing chances for self loop   1--->1,2
					remainder.remove(present_block);
					additions_successors_from_old.addAll(remainder);

				}
				System.out.println("gen:"+al_gen);
				System.out.println("Kill:"+al_kill);
				System.out.println("in_table" + in_table);
				System.out.println("out_table" + out_table);
				System.out.println("old=" + al_old);
				System.out.println("control_block_FLow_COPY=" + control_block_FLow_COPY);
				System.out.println("additions_successors_from_old=" + additions_successors_from_old);
				System.out.println(struc);
				System.out.println("************************************************************************************************");

				// Check for multiple definitions
				if (in_table.get(present_block).size() != 0) {
					ArrayList<String> multiple_def = Check_for_Multiple_Definitions(
							in_table.get(present_block), present_block);

					if (multiple_def.size() != 0) {
						System.out.println("THERE ARE MULTIPLE DEFINITIONS ...WE ARE NOW RECALCULATING THE WHOLE GEN/KILL/IN/OUT ");
						LinkedHashMap<Integer, String> modified_PHI_Block = Insert_PHI_Block(
								present_block, multiple_def,
								hmap_predecessor_list);
						// remove present struc block and add with phi value
						// block
						struc.set(present_block, modified_PHI_Block);
						System.out.println("With PHI Block inserted");
						System.out.println(struc);
						ArrayList<String> phi_def_check_num=new ArrayList<String>();
						
						for(Entry<Integer,String> phi_def:modified_PHI_Block.entrySet())
						{
							if( phi_def.getValue().contains("phi"))
							{
							String stmt=phi_def.getValue();
							stmt=stmt.substring(stmt.indexOf("let")+3, stmt.indexOf("=")).trim();
							phi_def_check_num.add(stmt);
							}
						}
						struc=Reformulate_BLOCKS_Renaming(struc,multiple_def,phi_def_check_num,hmap_predecessor_list.get(present_block));
						System.out.println("After reformulating: "+struc);
						System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
						HashMap<Integer, ArrayList<Integer>> gen_tab = Generate_Table(struc);
						HashMap<Integer, ArrayList<Integer>> kill_tab = Kill_Table(struc);
						
						Gen_Kill_UNION(gen_tab, kill_tab, successorsList);
						
						

					}

				}

			}
			else
			{   
				control_block_FLow_COPY.addAll(additions_successors_from_old);
				itr=control_block_FLow_COPY.listIterator(0);
			}

		} while (control_block_FLow_COPY.size()!= 0 || additions_successors_from_old.size()!=0 );
		
		gen_kill_in_out.put("GEN", gen_table);
		gen_kill_in_out.put("KILL", kill_table);
		gen_kill_in_out.put("IN", in_table);
		gen_kill_in_out.put("OUT", out_table);
		return gen_kill_in_out;
	}

	
	private static HashMap<Integer, ArrayList<Integer>> RemoveDuplicates_Outtable(
			HashMap<Integer, ArrayList<Integer>> out_table) {
		for(Entry<Integer,ArrayList<Integer>> out_tab_rem_dup:out_table.entrySet())
		{
			ArrayList<Integer> al=out_tab_rem_dup.getValue();
			ArrayList<Integer> al_rem_dup_out_Table=new ArrayList<Integer>();
			for(int j=0;j<al.size();j++)
			{
				if(!al_rem_dup_out_Table.contains(al.get(j)))
				    al_rem_dup_out_Table.add(al.get(j));
			}
			out_table.put(out_tab_rem_dup.getKey(), al_rem_dup_out_Table);
			
		}
		
		return out_table;
	}

	private static HashMap<Integer, ArrayList<Integer>> RemoveDuplicates_Intable(
			HashMap<Integer, ArrayList<Integer>> in_table) {
		for(Entry<Integer,ArrayList<Integer>> in_tab_rem_dup:in_table.entrySet())
		{
			ArrayList<Integer> al=in_tab_rem_dup.getValue();
			ArrayList<Integer> al_rem_dup_in_Table=new ArrayList<Integer>();
			for(int j=0;j<al.size();j++)
			{
				if(!al_rem_dup_in_Table.contains(al.get(j)))
				    al_rem_dup_in_Table.add(al.get(j));
			}
			in_table.put(in_tab_rem_dup.getKey(), al_rem_dup_in_Table);
			
			
		}
		
		return in_table;
		
	}

	public static ArrayList<HashMap<Integer, String>> Reformulate_BLOCKS_Renaming(
			ArrayList<HashMap<Integer, String>> struc_phi,
			ArrayList<String> multiple_def,
			ArrayList<String> phi_def_check_num,
			ArrayList<Integer> current_block_predList) {
		int occur = 0;

		occur = current_block_predList.size() + 1;
		int prev_occur=occur;
		int pred_count=0;

		for (int i = 0; i < multiple_def.size()-1; i++)
		{
			occur=0;
			for (int j = 0; j < struc_phi.size(); j++) 
			{
				HashMap<Integer, String> hmap = struc_phi.get(j);
			
					if (current_block_predList.contains(j)) 
					{
						pred_count++;
						occur=pred_count;
												
					}
					else 
					{
						occur=prev_occur;
						occur++;
						prev_occur=occur;
					}
				
				
			for (Entry<Integer, String> lines : hmap.entrySet())
			{
					if (!lines.getValue().contains("_")) {
						if (lines.getValue().contains("let")
								&& lines.getValue().contains(
										multiple_def.get(i))
								&& !lines.getValue().contains("_")
								&& !lines.getValue().contains("phi")) {

							String phi_num_check = phi_def_check_num.get(i);
							String avoid_def_phi = phi_num_check.substring(
									phi_num_check.indexOf("_") + 1,
									phi_num_check.length());
							Integer avoid_this_phi_num = Integer
									.parseInt(avoid_def_phi);
							String factor1 = lines.getValue();
							String oldfactor2 = factor1.substring(
									factor1.indexOf("let") + 3,
									factor1.indexOf("=")).trim();
							String factor2 = null;

							if (occur != avoid_this_phi_num) {
								factor2 = oldfactor2 + "_" + occur;
							} else {
								factor2 = oldfactor2 + "_" + occur;
								// occur=occur-1;
							}

							String lhs = factor1.substring(0,
									factor1.indexOf("=")).trim();
							lhs = lhs.replace(oldfactor2, factor2);
							String rhs = factor1.substring(
									factor1.indexOf("="), factor1.length())
									.trim();
							hmap.put(lines.getKey(), lhs + rhs);
						}
						if (lines.getValue().contains("if")
								&& lines.getValue().contains("goto")) {
							Pattern regex = Pattern
									.compile("(\\s*)(if)(\\s*)(\\w*)(\\s*)[<= < > => = ==](.*)");
							Matcher regexMatcher = regex.matcher(lines
									.getValue());

							String factor1 = lines.getValue().substring(
									lines.getValue().indexOf("(") + 1,
									lines.getValue().indexOf(")"));
							String tokens[] = factor1
									.split("\\<=|\\<|\\==|\\>|\\>=");
							// System.out.println(tokens[0]+"$$$$$$");
							String factor2 = tokens[0].trim() + "_" + occur
									+ " ";
							String factor3 = lines.getValue().replaceAll(
									tokens[0], factor2);
							hmap.put(lines.getKey(), factor3);
						}
					}
				}

				struc_phi.set(j, hmap);

			}
			
		}

		return struc_phi;
	}

	public static ArrayList<String> Check_for_Multiple_Definitions(ArrayList<Integer> arrayList, Integer present_block) 
	{
		HashMap<String,Integer> multi_def_symbol=new HashMap<String,Integer>();
				
		for (int i_arr = 0; i_arr < arrayList.size(); i_arr++)
		{
			for (int i = 0; i < struc.size(); i++)
			{
				HashMap<Integer, String> blocks = struc.get(i);
				for (Entry<Integer, String> hmap_block_line : blocks.entrySet())
				{					
					if (arrayList.get(i_arr) == hmap_block_line.getKey()) 
					{
						String line = hmap_block_line.getValue().trim();
						String def = line.substring(line.indexOf("let") + 3,line.indexOf("=")).trim();
						if(multi_def_symbol.containsKey(def))
						{
							for (Entry<String,Integer> multi : multi_def_symbol.entrySet())
							{	
								Integer freq = multi.getValue();
								freq++;
								multi_def_symbol.put(def,freq);
								
							}
							 
						}
						
						else
							multi_def_symbol.put(def,1);
					}
					
				}
				
			}
		}
		
		// check for entry.getValue() is >1
		ArrayList<String> multiDef_final=new ArrayList<String>();
		for(Entry<String,Integer> entry:multi_def_symbol.entrySet())
		{
			if(entry.getValue()>1)
			{
				multiDef_final.add(entry.getKey());
			}
		}
		
		return multiDef_final;
	}
		
			
	
	public static LinkedHashMap<Integer, String> Insert_PHI_Block(Integer present_block, ArrayList<String> multiple_def, HashMap<Integer, ArrayList<Integer>> hmap_predecessor_list) {

		
      	HashMap<Integer, String> phi_block = struc.get(present_block);
		LinkedHashMap<Integer, String> phi_block_afterADD=new LinkedHashMap<Integer, String>();
		int first_stmt_phi=1;
		int line_number_phi_start=0;
		int phi_var_count_row=-1;
		StringBuilder sb=new StringBuilder();
		
		
		ArrayList<String> list = new ArrayList<String>(multiple_def);
		
		for(Entry<Integer,String> phi_stmts:phi_block.entrySet())
		{			
			if(phi_stmts.getValue().contains("label"))
			{
				 phi_block_afterADD.put(phi_stmts.getKey(),phi_stmts.getValue().trim());
			     line_number_phi_start=phi_stmts.getKey();
			}
			if(first_stmt_phi==1)
			{
				for(int i=0;i<multiple_def.size();i++)
				{
					
					for(int j=0;j<hmap_predecessor_list.get(present_block).size();j++)
					{
						sb.append(list.get(i)+"_"+(j+1) +",");
					}
					int lhs_index=hmap_predecessor_list.get(present_block).size();
					Integer line_num_for_Phi=line_number_phi_start*10+i;
				  phi_block_afterADD.put((line_num_for_Phi), "let "+list.get(i)+"_"+(lhs_index+1)+"=phi("+sb+")");
				  sb.delete(0, sb.length());
				}
				first_stmt_phi++;
			}
			else
			{
				phi_block_afterADD.put(phi_stmts.getKey(), phi_stmts.getValue().trim());
			}
			
		}
		
	    return phi_block_afterADD;	
	}





   public static ArrayList<HashMap<Integer, String>> Constant_Propagation(ArrayList<HashMap<Integer, String>> struc) {
	
	   HashMap<String, Integer> const_def=new HashMap<String, Integer>();
	   for(int i=0;i<struc.size();i++)
	   {
		   HashMap<Integer, String> hmap_const=struc.get(i);
		   for(Entry<Integer, String> lines:hmap_const.entrySet()) 
		   {
			   if(lines.getValue().contains("let"))
			   {
				   String lhs=lines.getValue().substring(lines.getValue().indexOf("let")+3, lines.getValue().indexOf("=")).trim();
				   String rhs=lines.getValue().substring(lines.getValue().indexOf("=")+1, lines.getValue().length()).trim();
				   Pattern pattern = Pattern.compile("\\d");
					Matcher matcher = pattern.matcher(rhs);
					if(matcher.matches())
					{
						const_def.put(lhs, Integer.parseInt(rhs));
					}
			   }
			   else
			   {
				   hmap_const.put(lines.getKey(), lines.getValue());
			   }
		   }
	   }
	   
	   
	   
	   for(int i=0;i<struc.size();i++)
	   {
		   HashMap<Integer, String> hmap_const=struc.get(i);
		   HashMap<Integer, String> after_remove_let=new HashMap<Integer, String>();
		   for(Entry<Integer, String> lines:hmap_const.entrySet()) 
		   { 
			   
			  	    Pattern pattern = Pattern.compile("(\\s*)let(\\s*)(.*)(\\s*)=\\s*(\\d*)");
					Matcher matcher = pattern.matcher(lines.getValue());
					
					if(matcher.matches())
					{
						System.out.println(lines.getValue()+" Matched");
					}
					else
					{   
						for(Entry<String,Integer> def_check:const_def.entrySet())
						{
							
							if(lines.getValue().contains(def_check.getKey()))
							{
								lines.getValue().replace(def_check.getKey(), def_check.getValue().toString());
							}
						}
						after_remove_let.put(lines.getKey(), lines.getValue());
					}
			  
				   
			   }
		   struc.remove(i);
		   struc.add(i, after_remove_let);
		     
		   }  
	   
	return struc;
}

   

	public static ArrayList<HashMap<Integer, String>> Constant_folding(ArrayList<HashMap<Integer, String>> struc2)
	{
		
		for(int i=0;i<struc.size();i++)
		   {
			   HashMap<Integer, String> hmap_const=struc.get(i);
			   HashMap<Integer, String> after_modification=new HashMap<Integer, String>();
			for (Entry<Integer, String> lines : hmap_const.entrySet()) {
				if (lines.getValue().contains("if")	&& lines.getValue().contains("goto")) 
				{
					String factor1 = lines.getValue().substring(
							lines.getValue().indexOf("(") + 1,
							lines.getValue().indexOf(")"));
					int result = 0;

					Pattern pattern = Pattern.compile("s*(\\d*)s*.*s*(\\d*)");
					Matcher matcher = pattern.matcher(factor1);
					if (matcher.matches()) {
						System.out.println(factor1);
						// String lhs_exp=matcher.group(1);
						// String rhs_exp=matcher.group(2);
						// System.out.println(lhs_exp+"#####"+rhs_exp);
						result = Integer.valueOf(factor1) != null ? 1 : 0;
					}

					String replacement = lines.getValue().replaceAll(factor1,
							result + "");
					after_modification.put(lines.getKey(), replacement);
				}
				else
				{
					after_modification.put(lines.getKey(), lines.getValue());
				}
			}
			   struc.remove(i);
			   struc.add(i, after_modification);
			   
		   
		   }
		return struc;
	

}
}
		


