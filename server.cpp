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
static int connFd2;

void *task1 (void *dummyPt);

int main (int argc, char* argv[])
{
    int portNum, portNum2, listenFd, listenFd2;
    struct sockaddr_in svrAddress, clntAddress;
    struct sockaddr_in svrAddress2, clntAddress2;
    socklen_t szAddr, szAddr2; //store size of the address

    pthread_t threadArr[3];
    
    if (argc < 3)
    {
        cerr << "Args are: ./server <port>" << endl;
        return 0;
    }
    
    portNum = atoi(argv[1]);
    portNum2 = atoi(argv[2]);

    if((portNum > 65535) || (portNum < 2000) || (portNum2 > 65535) || (portNum2 < 2000))
    {
        cerr << "Please enter a port number between 2000 - 65535" << endl;
        return 0;
    }
    
    //create socket
    listenFd = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
    listenFd2 = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
    
    if(listenFd < 0)
    {
        cerr << "Cannot open socket 1" << endl;
        return 0;
    }
    
    if(listenFd2 < 0)
    {
        cerr << "Cannot open socket 2" << endl;
        return 0;
    }

    bzero((char*) &svrAddress, sizeof(svrAddress));
    
    svrAddress.sin_family = AF_INET;
    svrAddress.sin_addr.s_addr = INADDR_ANY;
    svrAddress.sin_port = htons(portNum);
    
    bzero((char*) &svrAddress2, sizeof(svrAddress2));
    
    svrAddress2.sin_family = AF_INET;
    svrAddress2.sin_addr.s_addr = INADDR_ANY;
    svrAddress2.sin_port = htons(portNum2);

    if(bind(listenFd, (struct sockaddr *)&svrAddress, sizeof(svrAddress)) < 0)
    {
        cerr << "Cannot bind first socket" << endl;
        return 0;
    }

    if(bind(listenFd2, (struct sockaddr *)&svrAddress2, sizeof(svrAddress2)) < 0)
    {
        cerr << "Cannot bind second socket" << endl;
        return 0;
    }

    
    listen(listenFd, 5);
    listen(listenFd2, 5);

    szAddr = sizeof(clntAddress);
    szAddr2 = sizeof(clntAddress2);

    int noThread = 0;

    while (noThread < 3)
    {
        socklen_t szAddr = sizeof(clntAddress);
        socklen_t szAddr2 = sizeof(clntAddress2);
        cout << "Listening" << endl;

        connFd = accept(listenFd, (struct sockaddr *)&clntAddress, &szAddr);
        connFd2 = accept(listenFd2, (struct sockaddr *)&clntAddress2, &szAddr2);

        if (connFd < 0)
        {
            cerr << "Cannot accept connection for first port" << endl;
            return 0;
        }
        else
        {
            cout << "Connection successful to first port" << endl;
        }
        
        if (connFd2 < 0)
        {
            cerr << "Cannot accept connection for second port" << endl;
            return 0;
        }
        else
        {
            cout << "Connection successful to second port" << endl;
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
    cout << "Thread No: " << pthread_self() << endl;
    char test[300];
    char test2[300];

    bzero(test, 301);
    bzero(test2, 301);
    
    bool flag = false;
    while(!flag)
    {    
        bzero(test, 301);  
        bzero(test2, 301);  

        read(connFd, test, 300);
        read(connFd2, test2, 300);

        string tester (test);
        string tester2 (test2);
        cout << tester << endl;
        cout << "second" <<tester2 << endl;

        
        if(tester == "exit")
            flag = true;
        if(tester2 == "exit")
            flag = true;
    }
    cout << "\nClosing thread and conn" << endl;
    close(connFd);
    close(connFd2);

}