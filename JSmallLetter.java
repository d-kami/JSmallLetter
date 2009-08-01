import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException ;

import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class JSmallLetter{
    public static void main(String[] args) throws Exception{
        if(args.length == 0){
            System.err.println("usage: java JSmallLetter filename");
            System.exit(1);
        }

        File file = new File(args[0]);
        if(!file.exists()){
            System.err.println(args[0] + " is not Found");
            System.exit(1);
        }

        char[] program = null;

        try{
            program = JSmallLetter.readAll(file);
        }catch(IOException e){
            System.err.println("ファイル読み込み中にエラーが発生しました");
            System.exit(1);
        }

        JSmallLetter.execute(program);
    }

    private static void execute(char[] program)
          throws Exception{
        int index = 0;
        char[] text = new char[4096];        
        int textIndex = -1;
        int number = -1;
        Stack<Object> stack = new Stack<Object>();
        List<Object> instanceTable = JSmallLetter.initInstanceTable(100);

        try{

            while(index != program.length){
                switch(program[index]){

                    case 'a':
                        textIndex++;
                        text[textIndex] = 'a';
                        break;

                    case 'n':
                        textIndex = -1;
                        number = 0;
                        break;

                    case 'x':
                        textIndex = -1;
                        number = Integer.MAX_VALUE;
                        break;

                    case 'i':
                        if(textIndex > -1)
                            text[textIndex]++;
                        else
                            number++;

                        break;

                    case 'd':
                        if(textIndex > -1)
                            text[textIndex]--;
                        else
                            number--;

                        break;

                     case 'u':
                         text[textIndex] = Character.toUpperCase(text[textIndex]);
                         break;

                    case 'l':
                         text[textIndex] = Character.toLowerCase(text[textIndex]);
                         break;

                    case 'p':
                        if(textIndex > -1){
                            stack.push(new String(text, 0, textIndex + 1));
                            textIndex = -1;
                        }
                        else{
                            stack.push(number);
                            number = -1;
                        }

                        break;

                    case 'o':
                        stack.pop();
                        break;

                    case 'c':
                       stack.push(JSmallLetter.callConstructor(stack));
                       break;

                    case 'm':
                        stack.push( JSmallLetter.invokeMethod(stack, false));
                        break;

                    case 's':
                        stack.push( JSmallLetter.invokeMethod(stack, true));
                        break;

                   case 'f':
                        stack.push(JSmallLetter.getField(stack, false));
                        break;

                   case 'g':
                        stack.push( JSmallLetter.getField(stack, true));
                        break;

                   case 'v':
                        int count = Integer.parseInt(stack.pop().toString());
                        instanceTable.set(count, stack.pop());

                        break;
                }

                index++;
            }
        }catch(Exception e){
            JSmallLetter.printErrorMessage(program[index], index, e);

            System.exit(1);
        }
    }

    private static void printErrorMessage(char c, int index, Exception e){
        System.err.println("execute " + c);
        System.err.println("index = " + index);
        System.err.println("exception = " + e.getClass().getName());
        System.err.println("message = " + e.getMessage());
    }

    private static char[] readAll(File file) throws IOException{
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder((int)file.length());

        try{
            br = new BufferedReader(new FileReader(file));

            String line;
            while((line = br.readLine()) != null)
                sb.append(line);

            br.close();
        }catch(IOException e){
            if(br != null){
                try{
                    br.close();
                }catch(IOException ioe){
                    ioe.printStackTrace();
                }
            }

            throw e;
        }

        return sb.toString().toCharArray();
    }

    private static List<Object> initInstanceTable(int capacity){
         List<Object> instanceTable = new ArrayList<Object>();
         Object obj = new Object();

         for(int i = 0; i < capacity; i++)
             instanceTable.add(obj);

         return instanceTable;
    }

    private static Object callConstructor(Stack<Object> stack)
            throws ClassNotFoundException,  NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        String className = stack.pop().toString();

        String parameter = stack.pop().toString();
        int parameterCount = 0;
        if(parameter.equals("type")){
            parameterCount = Integer.parseInt(stack.pop().toString());
            return JSmallLetter.callConstructorWithType(stack, className, parameterCount);
        }else{
            parameterCount = Integer.parseInt(parameter);
            return JSmallLetter.callConstructorWithoutType(stack, className, parameterCount);
        }
    }

    private static Object callConstructorWithType(Stack<Object> stack, String className, int parameterCount)
          throws ClassNotFoundException,  NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        Class<?>[] types = new Class<?>[parameterCount];
        Object[] args = new Object[parameterCount];

       for(int i = 0; i < parameterCount; i++){
            types[i] = Class.forName(stack.pop().toString());
        }

        for(int i = 0; i < parameterCount; i++){
            args[i] = stack.pop();;
        }

        return JSmallLetter.callConstructor(className, types, args);
    }

    private static Object callConstructorWithoutType(Stack<Object> stack, String className, int parameterCount)
            throws ClassNotFoundException,  NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        Class<?>[] types = new Class<?>[parameterCount];
        Object[] args = new Object[parameterCount];

        for(int i = 0; i < parameterCount; i++){
            Object arg = stack.pop();
            types[i] = arg.getClass();
            args[i] = arg;
        }

        return JSmallLetter.callConstructor(className, types, args);
    }

    private static Object callConstructor(String className, Class<?>[] types, Object[] args)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        Class<?> clazz = Class.forName(className);

        Constructor constructor = clazz.getConstructor(types);
        return constructor.newInstance(args);
    }

    private static Object invokeMethod(Stack<Object> stack, boolean isStatic)
             throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        Object target = null;
        if(!isStatic)
            target = stack.pop();

        String className = stack.pop().toString();
        String methodName = stack.pop().toString();

        String parameter = stack.pop().toString();
        int parameterCount = 0;
        if(parameter.equals("type")){
            parameterCount = Integer.parseInt(stack.pop().toString());
            return JSmallLetter.invokeMethodWithType(stack, target, className, methodName, parameterCount);
        }else{
            parameterCount = Integer.parseInt(parameter);
            return JSmallLetter.invokeMethodWithoutType(stack, target, className, methodName, parameterCount);
        }
    }

    private static Object invokeMethodWithType(Stack<Object> stack, Object target, String className, String methodName, int parameterCount)
            throws ClassNotFoundException,  NoSuchMethodException, IllegalAccessException, InvocationTargetException{

        Class<?>[] types = new Class<?>[parameterCount];
        Object[] args = new Object[parameterCount];

       for(int i = 0; i < parameterCount; i++){
            types[i] = Class.forName(stack.pop().toString());
        }

        for(int i = 0; i < parameterCount; i++){
            args[i] = stack.pop();;
        }

        return JSmallLetter.invokeMethod(className, target, methodName, types, args);
    }

    private static Object invokeMethodWithoutType(Stack<Object> stack, Object target, String className, String methodName, int parameterCount)
            throws ClassNotFoundException,  NoSuchMethodException, IllegalAccessException, InvocationTargetException{

        Class<?>[] types = new Class<?>[parameterCount];
        Object[] args = new Object[parameterCount];

        for(int i = 0; i < parameterCount; i++){
            Object arg = stack.pop();
            types[i] = arg.getClass();
            args[i] = arg;
        }

        return JSmallLetter.invokeMethod(className, target, methodName, types, args);
    }

    private static Object invokeMethod(String className, Object target, String methodName, Class<?>[] types, Object[] args)
              throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        Class<?> clazz = Class.forName(className);

        Method method = clazz.getMethod(methodName, types);
        return method.invoke(target, args);
    }

   private static Object getField(Stack<Object> stack, boolean isStatic)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException{

       Object target = null;
       if(!isStatic)
           target = stack.pop();

       String className = stack.pop().toString();
       String fieldName = stack.pop().toString();

       return JSmallLetter.getField(className, target, fieldName);
   }

    private static Object getField(String className, Object target, String fieldName)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException{

        Class<?> clazz = Class.forName(className);
        Field field = clazz.getField(fieldName);
        return field.get(target);
    }
}