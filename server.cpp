#include <cstring>
#include <string.h>
#include <unistd.h>
#include <stdio.h>
#include <netdb.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <iostream>
#include <stdlib.h>

using namespace std;

static int connFd;

void *task1 (void *dummyPt);

int main (int argc, char* argv[])
{
    int portNum, listenFd;
    struct sockaddr_in svrAddress, clntAddress;
    socklen_t szAddr; //store size of the address

    pthread_t threadArr[3];
    
    if (argc < 2)
    {
        cerr << "Args are: ./server <port>" << endl;
        return 0;
    }
    
    portNum = atoi(argv[1]);

    if((portNum > 65535) || (portNum < 2000))
    {
        cerr << "Please enter a port number between 2000 - 65535" << endl;
        return 0;
    }
    
    //create socket
    listenFd = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
    
    if(listenFd < 0)
    {
        cerr << "Cannot open socket 1" << endl;
        return 0;
    }


    bzero((char*) &svrAddress, sizeof(svrAddress));
    
    svrAddress.sin_family = AF_INET;
    svrAddress.sin_addr.s_addr = INADDR_ANY;
    svrAddress.sin_port = htons(portNum);
    

    if(bind(listenFd, (struct sockaddr *)&svrAddress, sizeof(svrAddress)) < 0)
    {
        cerr << "Cannot bind first socket" << endl;
        return 0;
    }
    
    listen(listenFd, 5);

    szAddr = sizeof(clntAddress);

    int noThread = 0;

    while (noThread < 3)
    {
        socklen_t szAddr = sizeof(clntAddress);
        printf ("Listening\n");

        connFd = accept(listenFd, (struct sockaddr *)&clntAddress, &szAddr);

        if (connFd < 0)
        {
            cerr << "Cannot accept connection for first port" << endl;
            return 0;
        }
        else
        {
            printf("Connection successful to first port\n");
        }

        pthread_create(&threadArr[noThread], NULL, task1, NULL); 
        
        noThread++;
    }
    
    for(int i = 0; i < 3; i++)
    {
        pthread_join(threadArr[i], NULL);
    }
    
    
}

void *task1 (void *dummyPt)
{
    printf("Thread No: %ld\n", pthread_self());

    bool flag = false;
    while(!flag)
    {   
        string tester; 

        char buf[1024];
        int numBytesRead = recv(connFd, buf, sizeof(buf), 0);
        /*if (numBytesRead > 0 || (buf[0] != '\n'))
        {
            for (int i=0; i<numBytesRead; i++)
            {
                char c = buf[i];
                if (c != '\n')
                {
                    tester += c;
                }
                else
                {
                    break;
                }  
            }
        }
        
        if(!tester.empty()){
            cout << tester << endl;
            send(connFd, buf, sizeof(buf), 0);

        }
        if(tester == "exit")
            flag = true;*/

        if(strcmp(buf, "exit") == 0)
        {
            cout << "\nClosing thread and conn" << endl;
            close(connFd);
        }
        else
        {
            printf("Client: %s\n", buf);
            send(connFd, buf, strlen(buf), 0);
            bzero(buf, sizeof(buf));
        }
    }
    

}