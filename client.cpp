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

int main (int argc, char* argv[])
{
    int listenFd, portNo;
    bool loop = false;
    struct sockaddr_in svrAdd;
    struct hostent *server;
    
    if(argc < 3)
    {
        cerr<<"Syntax : ./client <host name> <port>"<<endl;
        return 0;
    }
    
    portNo = atoi(argv[2]);
    
    if((portNo > 65535) || (portNo < 2000))
    {
        cerr<<"Please enter port number between 2000 - 65535"<<endl;
        return 0;
    }       
    
    //create client skt
    listenFd = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);    
    
    if(listenFd < 0)
    {
        cerr << "Cannot open socket" << endl;
        return 0;
    }
    
    server = gethostbyname(argv[1]);
    
    if(server == NULL)
    {
        cerr << "Host does not exist" << endl;
        return 0;
    }
    
    bzero((char *) &svrAdd, sizeof(svrAdd));
    svrAdd.sin_family = AF_INET;

    bcopy((char *) server -> h_addr, (char *) &svrAdd.sin_addr.s_addr, server -> h_length);
    
    svrAdd.sin_port = htons(portNo);
    
    int checker = connect(listenFd,(struct sockaddr *) &svrAdd, sizeof(svrAdd));
    
    if (checker < 0)
    {
        cerr << "Cannot connect!" << endl;
        return 0;
    }
    
    //send stuff to server
    while(1)
    {
        char s[1024];
        char rec[1024];

        //cin.clear();
        string tester;

        printf("Enter stuff: ");

        scanf("%s", &s[0]);
        //cin.ignore();
        send(listenFd, s, sizeof(s), 0);

        if(strcmp(s, "exit") == 0)
        {
            close(listenFd);
            printf("Disconnected from server\n");
            exit(1);
        }

        int numBytesRead = recv(listenFd, rec, sizeof(rec), 0);
        if (numBytesRead > 0 )
        {
            printf("Server: %s\n", rec);
        }
    
    }
}